import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javaparser.ParseResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JavaParserTool {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -cp javaparser.jar JavaParserTool <path_to_java_file>");
            System.exit(1);
        }

        File file = new File(args[0]);
        JavaParser parser = new JavaParser();

        try {
            ParseResult<CompilationUnit> parseResult = parser.parse(file);
            if (!parseResult.isSuccessful()) {
                System.err.println("Failed to parse file: " + file);
                System.exit(1);
            }
            CompilationUnit cu = parseResult.getResult().get();

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode result = mapper.createObjectNode();

            // Package
            String packageName = "";
            if (cu.getPackageDeclaration().isPresent()) {
                packageName = cu.getPackageDeclaration().get().getNameAsString();
            }
            result.put("package", packageName);

            // Classes
            ArrayNode classesNode = mapper.createArrayNode();
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
                ObjectNode classNode = mapper.createObjectNode();
                classNode.put("class_name", cls.getNameAsString());
                classNode.put("is_interface", cls.isInterface());

                // ── Annotations ──
                List<String> annotations = new ArrayList<>();
                for (AnnotationExpr ann : cls.getAnnotations()) {
                    annotations.add(ann.toString());
                }
                ArrayNode annotationsArray = mapper.createArrayNode();
                for (String ann : annotations) {
                    annotationsArray.add(ann);
                }
                classNode.set("annotations", annotationsArray);

                // ── Classification ──
                classNode.put("is_controller",
                    isAnnotationPresent(annotations, "@Controller", "@RestController"));
                classNode.put("is_exception_handler",
                    isAnnotationPresent(annotations, "@ControllerAdvice", "@RestControllerAdvice"));
                classNode.put("is_service",
                    isAnnotationPresent(annotations, "@Service"));
                classNode.put("is_repository",
                    isAnnotationPresent(annotations, "@Repository"));
                classNode.put("is_component",
                    isAnnotationPresent(annotations, "@Component"));
                classNode.put("is_configuration",
                    isAnnotationPresent(annotations, "@Configuration"));
                classNode.put("is_feign_client",
                    isAnnotationPresent(annotations, "@FeignClient"));

                // ── Implements ──
                ArrayNode implementsArray = mapper.createArrayNode();
                for (ClassOrInterfaceType impl : cls.getImplementedTypes()) {
                    implementsArray.add(impl.getNameAsString());
                }
                classNode.set("implements", implementsArray);

                // ── Extends ──
                ArrayNode extendsArray = mapper.createArrayNode();
                for (ClassOrInterfaceType ext : cls.getExtendedTypes()) {
                    extendsArray.add(ext.getNameAsString());
                }
                classNode.set("extends", extendsArray);

                // ── Fields ──
                ArrayNode fieldsNode = mapper.createArrayNode();
                for (FieldDeclaration field : cls.getFields()) {
                    for (VariableDeclarator var : field.getVariables()) {
                        ObjectNode fieldNode = mapper.createObjectNode();
                        fieldNode.put("name", var.getNameAsString());
                        fieldNode.put("type", var.getType().asString());

                        // Modifiers
                        fieldNode.put("is_final", field.isFinal());
                        fieldNode.put("is_static", field.isStatic());
                        fieldNode.put("is_private", field.isPrivate());
                        fieldNode.put("is_public", field.isPublic());
                        fieldNode.put("is_protected", field.isProtected());

                        // Field annotations
                        List<String> fieldAnnotations = new ArrayList<>();
                        for (AnnotationExpr ann : field.getAnnotations()) {
                            fieldAnnotations.add(ann.toString());
                        }
                        ArrayNode fieldAnnotationsArray = mapper.createArrayNode();
                        for (String ann : fieldAnnotations) {
                            fieldAnnotationsArray.add(ann);
                        }
                        fieldNode.set("annotations", fieldAnnotationsArray);

                        // Check for injection annotations
                        boolean isAutowired = isAnnotationPresent(fieldAnnotations,
                            "@Autowired", "@Inject", "@Resource");
                        boolean isValue = isAnnotationPresent(fieldAnnotations,
                            "@Value");
                        fieldNode.put("is_autowired", isAutowired);
                        fieldNode.put("is_value", isValue);

                        fieldsNode.add(fieldNode);
                    }
                }
                classNode.set("fields", fieldsNode);

                // ── Constructors ──
                ArrayNode constructorsNode = mapper.createArrayNode();
                for (ConstructorDeclaration ctor : cls.getConstructors()) {
                    ObjectNode ctorNode = mapper.createObjectNode();
                    ctorNode.put("name", ctor.getNameAsString());

                    ArrayNode paramsNode = mapper.createArrayNode();
                    ctor.getParameters().forEach(param -> {
                        ObjectNode paramNode = mapper.createObjectNode();
                        paramNode.put("name", param.getNameAsString());
                        paramNode.put("type", param.getType().asString());
                        paramNode.put("is_final", param.isFinal());

                        // Parameter annotations
                        ArrayNode paramAnns = mapper.createArrayNode();
                        param.getAnnotations().forEach(ann ->
                            paramAnns.add(ann.toString()));
                        paramNode.set("annotations", paramAnns);

                        paramsNode.add(paramNode);
                    });
                    ctorNode.set("parameters", paramsNode);

                    // Constructor annotations
                    ArrayNode ctorAnns = mapper.createArrayNode();
                    ctor.getAnnotations().forEach(ann ->
                        ctorAnns.add(ann.toString()));
                    ctorNode.set("annotations", ctorAnns);

                    constructorsNode.add(ctorNode);
                }
                classNode.set("constructors", constructorsNode);

                // ── Methods ──
                ArrayNode methodsNode = mapper.createArrayNode();
                cls.findAll(MethodDeclaration.class).forEach(method -> {
                    // Только методы непосредственно в этом классе
                    if (method.getParentNode().isPresent()
                        && method.getParentNode().get() != cls) {
                        return;
                    }

                    ObjectNode methodNode = mapper.createObjectNode();
                    methodNode.put("name", method.getNameAsString());
                    methodNode.put("return_type", method.getType().asString());

                    // Method annotations
                    List<String> methodAnnotations = new ArrayList<>();
                    for (AnnotationExpr ann : method.getAnnotations()) {
                        methodAnnotations.add(ann.toString());
                    }
                    ArrayNode methodAnnotationsArray = mapper.createArrayNode();
                    for (String ann : methodAnnotations) {
                        methodAnnotationsArray.add(ann);
                    }
                    methodNode.set("annotations", methodAnnotationsArray);

                    // Method parameters
                    ArrayNode methodParamsNode = mapper.createArrayNode();
                    method.getParameters().forEach(param -> {
                        ObjectNode paramNode = mapper.createObjectNode();
                        paramNode.put("name", param.getNameAsString());
                        paramNode.put("type", param.getType().asString());

                        ArrayNode paramAnns = mapper.createArrayNode();
                        param.getAnnotations().forEach(ann ->
                            paramAnns.add(ann.toString()));
                        paramNode.set("annotations", paramAnns);

                        // Check for request body, path variable, etc.
                        boolean isRequestBody = isAnnotationPresent(
                            toStringList(param.getAnnotations()), "@RequestBody");
                        boolean isPathVariable = isAnnotationPresent(
                            toStringList(param.getAnnotations()), "@PathVariable");
                        boolean isRequestParam = isAnnotationPresent(
                            toStringList(param.getAnnotations()), "@RequestParam");

                        paramNode.put("is_request_body", isRequestBody);
                        paramNode.put("is_path_variable", isPathVariable);
                        paramNode.put("is_request_param", isRequestParam);

                        if (isRequestBody) {
                            methodNode.put("request_body_type", param.getType().asString());
                        }

                        methodParamsNode.add(paramNode);
                    });
                    methodNode.set("parameters", methodParamsNode);

                    // HTTP method and path
                    String httpMethod = null;
                    String path = null;

                    if (isAnnotationPresent(methodAnnotations, "@GetMapping")) {
                        httpMethod = "GET";
                        path = extractPath(methodAnnotations, "@GetMapping");
                    } else if (isAnnotationPresent(methodAnnotations, "@PostMapping")) {
                        httpMethod = "POST";
                        path = extractPath(methodAnnotations, "@PostMapping");
                    } else if (isAnnotationPresent(methodAnnotations, "@PutMapping")) {
                        httpMethod = "PUT";
                        path = extractPath(methodAnnotations, "@PutMapping");
                    } else if (isAnnotationPresent(methodAnnotations, "@DeleteMapping")) {
                        httpMethod = "DELETE";
                        path = extractPath(methodAnnotations, "@DeleteMapping");
                    } else if (isAnnotationPresent(methodAnnotations, "@PatchMapping")) {
                        httpMethod = "PATCH";
                        path = extractPath(methodAnnotations, "@PatchMapping");
                    } else if (isAnnotationPresent(methodAnnotations, "@RequestMapping")) {
                        httpMethod = extractHttpMethodFromRequestMapping(methodAnnotations);
                        path = extractPath(methodAnnotations, "@RequestMapping");
                    }

                    // Exception Handler
                    boolean isExceptionHandler = isAnnotationPresent(
                        methodAnnotations, "@ExceptionHandler");
                    if (isExceptionHandler) {
                        methodNode.put("is_exception_handler", true);
                        ArrayNode exceptionTypesNode = mapper.createArrayNode();
                        for (String ann : methodAnnotations) {
                            if (ann.contains("@ExceptionHandler")) {
                                String[] types = ann.split("[\\{\\},()]");
                                for (String t : types) {
                                    t = t.trim();
                                    if (t.endsWith(".class")) {
                                        exceptionTypesNode.add(
                                            t.replace(".class", ""));
                                    }
                                }
                            }
                        }
                        methodNode.set("exception_types", exceptionTypesNode);
                    }

                    // Response status
                    if (isAnnotationPresent(methodAnnotations, "@ResponseStatus")) {
                        for (String ann : methodAnnotations) {
                            if (ann.contains("@ResponseStatus")) {
                                methodNode.put("response_status", ann);
                            }
                        }
                    }

                    methodNode.put("http_method", httpMethod);
                    methodNode.put("path", path);

                    methodsNode.add(methodNode);
                });
                classNode.set("methods", methodsNode);

                classesNode.add(classNode);
            });
            result.set("classes", classesNode);

            System.out.println(mapper.writeValueAsString(result));

        } catch (Exception e) {
            System.err.println("Error parsing file: " + e.getMessage());
            System.exit(1);
        }
    }

    private static boolean isAnnotationPresent(
            List<String> annotations, String... targetAnnotations) {
        for (String ann : annotations) {
            for (String target : targetAnnotations) {
                if (ann.contains(target)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String extractPath(
            List<String> annotations, String targetAnnotation) {
        for (String ann : annotations) {
            if (ann.contains(targetAnnotation)) {
                // Try to find path in quotes
                int start = ann.indexOf('"');
                if (start != -1) {
                    int end = ann.indexOf('"', start + 1);
                    if (end != -1) {
                        return ann.substring(start + 1, end);
                    }
                }
                // If no path specified, return empty string (root)
                if (ann.matches("@\\w+Mapping\\s*$")
                    || ann.matches("@\\w+Mapping\\(\\)")) {
                    return "";
                }
            }
        }
        return "";
    }

    private static String extractHttpMethodFromRequestMapping(
            List<String> annotations) {
        for (String ann : annotations) {
            if (ann.contains("@RequestMapping")) {
                String upper = ann.toUpperCase();
                if (upper.contains("POST")) return "POST";
                if (upper.contains("PUT")) return "PUT";
                if (upper.contains("DELETE")) return "DELETE";
                if (upper.contains("PATCH")) return "PATCH";
            }
        }
        return "GET"; // default
    }

    private static List<String> toStringList(
            com.github.javaparser.ast.NodeList<AnnotationExpr> annotations) {
        List<String> result = new ArrayList<>();
        for (AnnotationExpr ann : annotations) {
            result.add(ann.toString());
        }
        return result;
    }
}