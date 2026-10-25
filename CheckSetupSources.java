/* Copyright (C) 2026 petalOS; SPDX-License-Identifier: Apache-2.0 */
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

// Parse sources and catch local override visibility mistakes; no Android build.
public class CheckSetupSources {
    static class Type {
        final ClassTree tree;
        final List<String> parents = new ArrayList<>();
        final Map<String, MethodTree> methods = new HashMap<>();

        Type(ClassTree tree) {
            this.tree = tree;
            if (tree.getExtendsClause() != null) parents.add(simple(tree.getExtendsClause()));
            for (Tree parent : tree.getImplementsClause()) parents.add(simple(parent));
            for (Tree member : tree.getMembers()) {
                if (member instanceof MethodTree method
                        && !method.getName().contentEquals("<init>")) {
                    String signature = method.getName() + "(" + method.getParameters().stream()
                            .map(parameter -> parameter.getType().toString())
                            .collect(java.util.stream.Collectors.joining(",")) + ")";
                    methods.put(signature, method);
                }
            }
        }
    }

    static String simple(Tree tree) {
        String type = tree.toString().replaceAll("<.*>", "");
        return type.substring(type.lastIndexOf('.') + 1);
    }

    static int visibility(MethodTree method, boolean inInterface) {
        Set<Modifier> flags = method.getModifiers().getFlags();
        if (flags.contains(Modifier.PRIVATE)) return 0;
        if (inInterface || flags.contains(Modifier.PUBLIC)) return 3;
        return flags.contains(Modifier.PROTECTED) ? 2 : 1;
    }

    static void check(String name, Type type, Type parent, Map<String, Type> types,
            Set<String> visited, List<String> errors) {
        if (!visited.add(parent.tree.getSimpleName().toString())) return;
        for (var method : type.methods.entrySet()) {
            MethodTree inherited = parent.methods.get(method.getKey());
            if (inherited == null || inherited.getModifiers().getFlags().contains(Modifier.STATIC)) {
                continue;
            }
            int required = visibility(inherited, parent.tree.getKind() == Tree.Kind.INTERFACE);
            if (required >= 2 && visibility(method.getValue(), false) < required) {
                errors.add(name + "." + method.getKey() + " weakens inherited visibility from "
                        + parent.tree.getSimpleName());
            }
        }
        for (String ancestor : parent.parents) {
            if (types.containsKey(ancestor)) {
                check(name, type, types.get(ancestor), types, visited, errors);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) throw new IllegalArgumentException("Supply source directories");
        List<Path> sources = new ArrayList<>();
        for (String arg : args) {
            try (var files = Files.walk(Path.of(arg))) {
                files.filter(path -> path.toString().endsWith(".java")).forEach(sources::add);
            }
        }
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("A host JDK is required");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        Map<String, Type> types = new HashMap<>();
        List<String> errors = new ArrayList<>();
        try (var manager = compiler.getStandardFileManager(diagnostics, null, null)) {
            var task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    List.of("-proc:none"), null, manager.getJavaFileObjectsFromPaths(sources));
            for (CompilationUnitTree unit : task.parse()) {
                new TreeScanner<Void, Void>() {
                    @Override public Void visitClass(ClassTree tree, Void unused) {
                        if (!tree.getSimpleName().isEmpty()) {
                            types.put(tree.getSimpleName().toString(), new Type(tree));
                        }
                        return super.visitClass(tree, unused);
                    }
                }.scan(unit, null);
            }
            for (var diagnostic : diagnostics.getDiagnostics()) {
                if (diagnostic.getKind() == Diagnostic.Kind.ERROR) errors.add(diagnostic.toString());
            }
        }
        for (var entry : types.entrySet()) {
            for (String parent : entry.getValue().parents) {
                if (types.containsKey(parent)) {
                    check(entry.getKey(), entry.getValue(), types.get(parent), types,
                            new HashSet<>(), errors);
                }
            }
        }
        if (!errors.isEmpty()) throw new IllegalStateException(String.join("\n", errors));
        System.out.println("PASS: parsed " + sources.size()
                + " Java sources and checked local inherited method visibility.");
        System.out.println("No Android type resolution, class output or application build.");
    }
}
