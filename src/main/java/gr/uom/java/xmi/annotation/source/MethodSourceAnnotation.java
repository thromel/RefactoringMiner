package gr.uom.java.xmi.annotation.source;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.StatementObject;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.annotation.MarkerAnnotation;
import gr.uom.java.xmi.annotation.SingleMemberAnnotation;

import java.util.*;
import java.util.stream.Collectors;

public class MethodSourceAnnotation extends SourceAnnotation implements SingleMemberAnnotation, MarkerAnnotation {
    public static final String ANNOTATION_TYPENAME = "MethodSource";
    private final UMLOperation annotatedOperation;
    private final UMLAbstractClass annotatedClass;
    private final UMLModel sourceModel;

    public MethodSourceAnnotation(UMLAnnotation annotation, UMLOperation operation, UMLAbstractClass declaringClass) {
        this(annotation, operation, declaringClass, null);
    }

    public MethodSourceAnnotation(UMLAnnotation annotation, UMLOperation operation, UMLAbstractClass declaringClass, UMLModel sourceModel) {
        super(annotation, ANNOTATION_TYPENAME);
        this.annotatedOperation = operation;
        this.annotatedClass = declaringClass;
        this.sourceModel = sourceModel;
        List<String> values = getValue();
        for (String value : values) {
            MethodSourceReference reference = MethodSourceReference.parse(value);
            UMLAbstractClass sourceClass = resolveSourceClass(reference);
            if (sourceClass == null) {
                continue;
            }
            List<UMLOperation> sourceMethods = findSourceMethods(sourceClass, reference);
            for (UMLOperation sourceMethod : sourceMethods) {
                collectTestParameters(sourceMethod);
            }
        }
    }

    @Override
    public List<String> getValue() {
        if(annotation.isMarkerAnnotation()) {
            return Collections.singletonList(annotatedOperation.getName());
        }
        else if (annotation.isNormalAnnotation()) {
            ArrayList<String> values = new ArrayList<>();
            for (AbstractExpression value : annotation.getMemberValuePairs().values()) {
                values.addAll(extractMethodSourceValues(value, new LinkedHashSet<>()));
            }
            return values;
        }
        else {
            return extractMethodSourceValues(annotation.getValue(), new LinkedHashSet<>());
        }
    }

    private List<String> extractMethodSourceValues(AbstractExpression expr, Set<String> visitedReferences) {
        List<String> resolvedValues = new ArrayList<>();
        for (LeafExpression typeLiteral : expr.getTypeLiterals()) {
            resolvedValues.add(typeLiteral.getString());
        }
        for (LeafExpression stringLiteral : expr.getStringLiterals()) {
            resolvedValues.add(stringLiteral.getString().replace("\"", ""));
        }
        resolvedValues.addAll(resolveConstantReference(expr.getString(), visitedReferences));
        for (LeafExpression variable : expr.getVariables()) {
            resolvedValues.addAll(resolveConstantReference(variable.getString(), visitedReferences));
        }
        if (resolvedValues.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(new LinkedHashSet<>(resolvedValues));
    }

    private List<String> resolveConstantReference(String reference, Set<String> visitedReferences) {
        if (reference == null || reference.isBlank()) {
            return Collections.emptyList();
        }
        String normalizedReference = normalizeReference(reference);
        if (normalizedReference.isEmpty() || !visitedReferences.add(normalizedReference)) {
            return Collections.emptyList();
        }
        for (UMLAttribute attribute : annotatedClass.getAttributes()) {
            if (attribute.getName().equals(normalizedReference) && attribute.getVariableDeclaration() != null && attribute.getVariableDeclaration().getInitializer() != null) {
                return extractMethodSourceValues(attribute.getVariableDeclaration().getInitializer(), visitedReferences);
            }
        }
        return Collections.emptyList();
    }

    private UMLAbstractClass resolveSourceClass(MethodSourceReference reference) {
        if (reference.className == null || reference.className.isBlank()) {
            return annotatedClass;
        }
        if (sourceModel == null) {
            return null;
        }
        for (UMLClass umlClass : sourceModel.getClassList()) {
            if (umlClass.getName().equals(reference.className) || umlClass.getName().endsWith("." + reference.className)) {
                return umlClass;
            }
        }
        return null;
    }

    private List<UMLOperation> findSourceMethods(UMLAbstractClass sourceClass, MethodSourceReference reference) {
        List<UMLOperation> sameNameMethods = sourceClass.getOperations().stream()
                .filter(op -> op.getName().equals(reference.methodName))
                .collect(Collectors.toList());
        if (reference.signatureSpecified) {
            sameNameMethods.removeIf(op -> !hasMatchingSignature(op, reference.parameterTypes));
        }
        for (int maxIterations = sameNameMethods.size(); sameNameMethods.size() > 1 && maxIterations-- > 0; ) {
            for (Iterator<UMLOperation> iterator = sameNameMethods.iterator(); iterator.hasNext(); ) {
                UMLOperation method = iterator.next();
                if (method.getAnnotations().containsAll(annotatedOperation.getAnnotations())) {
                    iterator.remove();
                    break;
                }
                if (method.equalSignature(annotatedOperation)) {
                    iterator.remove();
                    break;
                }
            }
        }
        return sameNameMethods;
    }

    private boolean hasMatchingSignature(UMLOperation operation, List<String> parameterTypes) {
        List<UMLParameter> parameters = operation.getParametersWithoutReturnType();
        if (parameters.size() != parameterTypes.size()) {
            return false;
        }
        for (int i = 0; i < parameterTypes.size(); i++) {
            UMLType parameterType = parameters.get(i).getType();
            String expectedType = normalizeTypeName(parameterTypes.get(i));
            if (!matchesType(parameterType, expectedType)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesType(UMLType parameterType, String expectedType) {
        if (expectedType.isEmpty()) {
            return false;
        }
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(normalizeTypeName(parameterType.toQualifiedString()));
        candidates.add(normalizeTypeName(parameterType.toString()));
        candidates.add(normalizeTypeName(parameterType.getClassType()));
        String classType = parameterType.getClassType();
        int simpleNameIndex = classType.lastIndexOf('.');
        if (simpleNameIndex != -1) {
            candidates.add(normalizeTypeName(classType.substring(simpleNameIndex + 1)));
        }
        return candidates.contains(expectedType);
    }

    private void collectTestParameters(UMLOperation sourceMethod) {
        if (sourceMethod.getBody() == null) {
            return;
        }
        Optional<VariableDeclaration> returnedVarCandidates = sourceMethod.getBody().getAllVariableDeclarations().stream().filter(v -> sourceMethod.getReturnParameter().getType().equals(v.getType())).findAny();
        if (returnedVarCandidates.isPresent()) {
            returnedVarCandidates.get().getStatementsInScopeUsingVariable().stream()
                    .flatMap(stmt -> stmt.getStringLiterals().stream())
                    .map(str -> str.getString())
                    .collect(Collectors.joining(System.getProperty("line.separator")));
        } else {
            Optional<StatementObject> stmtCandidate = sourceMethod.getBody().getCompositeStatement().getStatements().stream()
                    .filter(s -> s instanceof StatementObject)
                    .map(s -> (StatementObject) s)
                    .filter(StatementObject::isLastStatement)
                    .findAny();
            if (stmtCandidate.isPresent()) {
                AbstractCall call = stmtCandidate.get().invocationCoveringEntireFragment();
                if(call != null && call.getName().equals("of")) {
                    for(AbstractCall nestedCall : stmtCandidate.get().getMethodInvocations()) {
                        if(nestedCall.getExpression() != null && !nestedCall.getExpression().equals("Stream") && nestedCall.getName().equals("of")) {
                            testParameters.add(nestedCall.arguments());
                            List<LeafExpression> leafExpressions = new ArrayList<>();
                            for(String arg : nestedCall.arguments()) {
                                List<LeafExpression> matches = stmtCandidate.get().findExpression(arg);
                                for(LeafExpression match : matches) {
                                    if(nestedCall.getLocationInfo().subsumes(match.getLocationInfo())) {
                                        leafExpressions.add(match);
                                    }
                                }
                            }
                            testParameterLeafExpressions.add(leafExpressions);
                        }
                    }
                }
            }
        }
    }

    private static String normalizeReference(String reference) {
        String normalized = reference.trim();
        if (normalized.startsWith("this.")) {
            normalized = normalized.substring("this.".length());
        }
        int fieldAccessIndex = normalized.lastIndexOf('.');
        if (fieldAccessIndex != -1 && !normalized.contains("#")) {
            normalized = normalized.substring(fieldAccessIndex + 1);
        }
        return normalized;
    }

    private static String normalizeTypeName(String typeName) {
        String normalized = typeName.replace("...", "[]").replace(" ", "").trim();
        int genericStart = normalized.indexOf('<');
        if (genericStart != -1) {
            normalized = normalized.substring(0, genericStart);
        }
        return normalized;
    }

    private static List<String> splitSignature(String signature) {
        if (signature.isBlank()) {
            return Collections.emptyList();
        }
        List<String> parts = new ArrayList<>();
        int genericDepth = 0;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < signature.length(); i++) {
            char c = signature.charAt(i);
            if (c == '<') {
                genericDepth++;
            } else if (c == '>') {
                genericDepth--;
            } else if (c == ',' && genericDepth == 0) {
                parts.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        if (current.length() > 0) {
            parts.add(current.toString().trim());
        }
        return parts;
    }

    private static class MethodSourceReference {
        private final String className;
        private final String methodName;
        private final List<String> parameterTypes;
        private final boolean signatureSpecified;

        private MethodSourceReference(String className, String methodName, List<String> parameterTypes, boolean signatureSpecified) {
            this.className = className;
            this.methodName = methodName;
            this.parameterTypes = parameterTypes;
            this.signatureSpecified = signatureSpecified;
        }

        private static MethodSourceReference parse(String rawReference) {
            String reference = rawReference.trim();
            String className = null;
            String methodReference = reference;
            if (reference.contains("#")) {
                int separatorIndex = reference.indexOf('#');
                className = reference.substring(0, separatorIndex).trim();
                methodReference = reference.substring(separatorIndex + 1).trim();
            }
            boolean signatureSpecified = methodReference.contains("(") && methodReference.endsWith(")");
            List<String> parameterTypes = Collections.emptyList();
            String methodName = methodReference;
            if (signatureSpecified) {
                int signatureStart = methodReference.indexOf('(');
                methodName = methodReference.substring(0, signatureStart).trim();
                parameterTypes = splitSignature(methodReference.substring(signatureStart + 1, methodReference.length() - 1));
            }
            return new MethodSourceReference(className, methodName, parameterTypes, signatureSpecified);
        }
    }
}
