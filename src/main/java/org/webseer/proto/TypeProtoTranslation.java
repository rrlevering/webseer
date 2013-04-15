package org.webseer.proto;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import org.webseer.model.meta.Field;
import org.webseer.model.meta.InputPoint;
import org.webseer.model.meta.OutputPoint;
import org.webseer.model.meta.Transformation;
import org.webseer.model.meta.Type;

import com.google.common.collect.Iterables;

/**
 * This class can be used to translate in-between webseer's internal DB representation of types and protocol buffers,
 * which are used for RPC/network communication.
 */
public class TypeProtoTranslation {

	/**
	 * Generates code representing a specific type in webseer.
	 */
	public static String generateProtoCode(Type type) {
		StringBuilder protoFile = new StringBuilder();
		Stack<Type> toDefine = new Stack<Type>();
		toDefine.add(type);
		Set<Type> alreadyDefined = new HashSet<Type>();
		while (!toDefine.isEmpty()) {
			Type current = toDefine.pop();
			if (alreadyDefined.contains(current)) {
				continue;
			}
			alreadyDefined.add(current);

			int i = 1;
			protoFile.append("package ").append(type.getPackage()).append(";\n\n");
			protoFile.append("message ").append(type.getSimpleName()).append(" {\n");
			for (Field field : type.getFields()) {
				if (!field.getType().isPrimitive()) {
					toDefine.add(field.getType());
				}
				String typeName = getTypeName(field.getType());
				String muliplicity = field.isRepeated() ? "repeated" : "optional";
				protoFile.append("\t").append(muliplicity).append(" ").append(typeName).append(" ")
						.append(field.getName()).append(" = ").append(i++).append(";\n");
			}
			protoFile.append("}");
		}
		return protoFile.toString();
	}

	private static String getTypeName(Type type) {
		return type.getSimpleName();
	}

	/**
	 * Generates code representing the service call to a particular transformation. Generates generic request/response
	 * protos wrapping the inputs and outputs of the transformation and generates a protocol buffer service definition.
	 */
	public static String generateProtoCode(Transformation transformation) {
		StringBuilder protoFile = new StringBuilder();
		String name = transformation.getSimpleName();
		String request = Iterables.isEmpty(transformation.getInputPoints()) ? "" : (name + "Request");
		String response = Iterables.isEmpty(transformation.getOutputPoints()) ? "" : (name + "Response");
		protoFile.append("service ").append(name).append("Service {\n");
		protoFile.append("\t").append(name).append("(").append(request).append(") returns (").append(response)
				.append(")\n");
		protoFile.append("}\n\n");
		if (!request.isEmpty()) {
			int i = 1;
			protoFile.append("message ").append(request).append(" {\n");
			for (InputPoint input : transformation.getInputPoints()) {
				String typeName = getTypeName(input.getField().getType());
				String muliplicity = input.getField().isRepeated() ? "repeated" : "optional";
				protoFile.append("\t").append(muliplicity).append(" ").append(typeName).append(" ")
						.append(input.getField().getName()).append(" = ").append(i++).append(";\n");
			}
			protoFile.append("}\n\n");
		}
		if (!response.isEmpty()) {
			int i = 1;
			protoFile.append("message ").append(response).append(" {\n");
			for (OutputPoint output : transformation.getOutputPoints()) {
				String typeName = getTypeName(output.getField().getType());
				String muliplicity = output.getField().isRepeated() ? "repeated" : "optional";
				protoFile.append("\t").append(muliplicity).append(" ").append(typeName).append(" ")
						.append(output.getField().getName()).append(" = ").append(i++).append(";\n");
			}
			protoFile.append("}\n\n");
		}
		return protoFile.toString();
	}

}
