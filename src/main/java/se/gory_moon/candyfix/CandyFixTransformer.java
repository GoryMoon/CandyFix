package se.gory_moon.candyfix;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.classloading.FMLForgePlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * The base setup of this class is by hilburn
 * It's a very nice way of setting up the transformer
 */
public class CandyFixTransformer implements IClassTransformer, Opcodes {

    private static final Logger LOGGER = LogManager.getLogger("CandyFix");

    private enum TransformType {
        METHOD, FIELD, INNER_CLASS, MODIFY, MAKE_PUBLIC, DELETE, ADD
    }

    private enum Transformer {
        /**
         * Adds a pos.getY >= 0 check before the other checks like below.<br>
         * Original method
         * <pre>{@code
         * private boolean isAirOrLiquid(World world, BlockPos pos) {
         *     return world.isAirBlock(pos) || world.getBlockState(pos).getMaterial().isLiquid();
         * }
         * }</pre>
         * Patched method
         * <pre>{@code
         * private boolean isAirOrLiquid(World world, BlockPos pos) {
         *     return pos.getY() >= 0 && (world.isAirBlock(pos) || world.getBlockState(pos).getMaterial().isLiquid());
         * }
         * }</pre>
         */
        ADD_Y_CHECK("isAirOrLiquid", "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)Z") {
            @Override
            protected InsnList modifyInstructions(InsnList list) {
                AbstractInsnNode node = list.getFirst().getNext();
                AbstractInsnNode start = null;

                while (node.getOpcode() != GOTO) {
                    if (node.getType() == AbstractInsnNode.LINE) {
                        start = node.getNext();
                    }
                    node = node.getNext();
                }
                node = node.getNext();

                if (start != null) {
                    list.insertBefore(start, new VarInsnNode(ALOAD, 2));
                    list.insertBefore(start, new MethodInsnNode(INVOKEVIRTUAL, "net/minecraft/util/math/BlockPos", FMLForgePlugin.RUNTIME_DEOBF ? "func_177956_o": "getY", "()I", false));
                    list.insertBefore(start, new JumpInsnNode(IFLT, (LabelNode) node));
                }
                return list;
            }
        },
        /**
         * Helper for printing the structure of a method
         * First parameter is the method name and second is the description of it
         * Change the boolean printCompact for either all code easily copy and pastable to somewhere else
         * or separated with the name of the class to use for the specific call
         */
        PRINT("isAirOrLiquid", "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)Z") {
            private Printer printer;
            private TraceMethodVisitor mp;

            public String insnToString(AbstractInsnNode insn){
                insn.accept(mp);
                StringWriter sw = new StringWriter();
                printer.print(new PrintWriter(sw));
                printer.getText().clear();
                return sw.toString();
            }

            @Override
            protected InsnList modifyInstructions(InsnList list) {
                printer = new Textifier();
                mp = new TraceMethodVisitor(printer);
                boolean printCompact = true;
                for (int i = 0; i < list.size(); i++) {
                    if (printCompact) {
                        System.out.print(insnToString(list.get(i)));
                    } else {
                        LOGGER.info(String.valueOf(list.get(i)));
                        LOGGER.info(insnToString(list.get(i)));
                    }
                }
                return super.modifyInstructions(list);
            }
        };

        protected String name;
        protected String args;
        protected TransformType type;
        protected TransformType action;

        Transformer(String name) {
            this(name, "", TransformType.INNER_CLASS, TransformType.MAKE_PUBLIC);
        }

        Transformer(String name, String args) {
            this(name, args, TransformType.METHOD, TransformType.MODIFY);
        }

        Transformer(String name, String args, TransformType type, TransformType action) {
            this.name = name;
            this.args = args;
            this.type = type;
            this.action = action;
        }

        protected InsnList modifyInstructions(InsnList list)
        {
            return list;
        }

        private static InsnList replace(InsnList list, String toReplace, String replace) {
            AbstractInsnNode node = list.getFirst();
            InsnList result = new InsnList();
            while (node != null) {
                result.add(checkReplace(node, toReplace, replace));
                node = node.getNext();
            }
            return result;
        }

        public String getName()
        {
            return name;
        }

        public String getArgs()
        {
            return args;
        }

        protected void methodTransform(ClassNode node) {
            MethodNode methodNode = getMethod(node);
            if (methodNode == null && action == TransformType.ADD) {
                methodNode = new MethodNode(ASM4, ACC_PUBLIC, name, args, null, null);
                node.methods.add(methodNode);
            }

            if (methodNode != null) {
                switch (action) {
                    case ADD:
                    case MODIFY:
                        methodNode.instructions = modifyInstructions(methodNode.instructions);
                        break;
                    case DELETE:
                        node.methods.remove(methodNode);
                        break;
                    case MAKE_PUBLIC:
                        methodNode.access = (methodNode.access & ~7) ^ 1;
                }
                complete();
            }
        }

        private void fieldTransform(ClassNode node) {
            FieldNode fieldNode = getField(node);
            if (fieldNode != null) {
                switch (action) {
                    case MODIFY:
                        modifyField(fieldNode);
                        break;
                    case DELETE:
                        node.fields.remove(fieldNode);
                        break;
                    case MAKE_PUBLIC:
                        fieldNode.access = (fieldNode.access & ~7) ^ 1;
                }
                complete();
            }
        }

        private void modifyField(FieldNode fieldNode) {
        }


        private void innerClassTransform(ClassNode node) {
            InnerClassNode innerClassNode = getInnerClass(node);
            if (innerClassNode != null) {
                switch (action) {
                    case MODIFY:
                        modifyInnerClass(innerClassNode);
                        break;
                    case DELETE:
                        node.innerClasses.remove(innerClassNode);
                        break;
                    case MAKE_PUBLIC:
                        innerClassNode.access = (innerClassNode.access & ~7) ^ 1;
                }
                complete();
            }
        }

        private void modifyInnerClass(InnerClassNode innerClassNode) {
        }

        public void transform(ClassNode node) {
            switch (this.type) {
                case METHOD:
                    methodTransform(node);
                    return;
                case FIELD:
                    fieldTransform(node);
                    return;
                case INNER_CLASS:
                    innerClassTransform(node);
            }
        }

        private static AbstractInsnNode checkReplace(AbstractInsnNode node, String toReplace, String replace) {
            if (node instanceof TypeInsnNode && ((TypeInsnNode)node).desc.equals(toReplace)) {
                return new TypeInsnNode(NEW, replace);
            } else if (node instanceof MethodInsnNode && ((MethodInsnNode)node).owner.contains(toReplace)) {
                return new MethodInsnNode(node.getOpcode(), replace, ((MethodInsnNode)node).name, ((MethodInsnNode)node).desc, false);
            }
            return node;
        }

        public void complete()
        {
            LOGGER.info("Applied " + this + " transformer");
        }

        public MethodNode getMethod(ClassNode classNode) {
            for (MethodNode method : classNode.methods) {
                if (method.name.equals(getName()) && method.desc.equals(getArgs())) {
                    return method;
                }
            }
            for (MethodNode method : classNode.methods) {
                if (method.desc.equals(getArgs())) {
                    return method;
                }
            }
            return null;
        }

        public FieldNode getField(ClassNode classNode) {
            for (FieldNode field : classNode.fields) {
                if (field.name.equals(getName()) && field.desc.equals(getArgs())) {
                    return field;
                }
            }
            return null;
        }

        public InnerClassNode getInnerClass(ClassNode classNode) {
            String name = classNode.name + "$" + getName();
            for (InnerClassNode inner : classNode.innerClasses) {
                if (name.equals(inner.name)) {
                    return inner;
                }
            }
            return null;
        }
    }

    private enum ClassName {
        GUI_WORKBENCH("com.ochotonida.candymod.world.worldgen.WorldGenGummyWorm", Transformer.ADD_Y_CHECK);

        private String name;
        private Transformer[] transformers;

        ClassName(String name, Transformer... transformers) {
            this.name = name;
            this.transformers = transformers;
        }

        public String getName()
        {
            return name;
        }

        public Transformer[] getTransformers()
        {
            return transformers;
        }

        public byte[] transform(byte[] bytes) {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(bytes);
            classReader.accept(classNode, 0);

            LOGGER.info("Applying Transformer" + (transformers.length > 1 ? "s " : " ") + "to " + getName());

            for (Transformer transformer : getTransformers()) {
                transformer.transform(classNode);
            }

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            classNode.accept(writer);
            return writer.toByteArray();
        }
    }

    private static Map<String, ClassName> classMap = new HashMap<String, ClassName>();

    static {
        for (ClassName className : ClassName.values()) classMap.put(className.getName(), className);
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        ClassName clazz = classMap.get(name);
        if (clazz != null) {
            basicClass = clazz.transform(basicClass);
            classMap.remove(name);
        }
        return basicClass;
    }
}
