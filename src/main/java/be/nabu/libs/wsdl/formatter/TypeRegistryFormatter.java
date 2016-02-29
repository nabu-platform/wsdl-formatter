package be.nabu.libs.wsdl.formatter;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;

import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.api.TypeRegistry;
import be.nabu.libs.types.definition.xsd.XSDDefinitionMarshaller;

public class TypeRegistryFormatter {
	
	public List<Document> format(TypeRegistry registry) {
		List<Document> documents = new ArrayList<Document>();
		XSDDefinitionMarshaller marshaller = new XSDDefinitionMarshaller();
		for (String namespace : registry.getNamespaces()) {
			for (Element<?> element : registry.getElements(namespace)) {
				marshaller.define(element);
			}
			for (SimpleType<?> simpleType : registry.getSimpleTypes(namespace)) {
				marshaller.define(simpleType);
			}
			for (ComplexType complexType : registry.getComplexTypes(namespace)) {
				marshaller.define(complexType);
			}
		}
		documents.add(marshaller.getSchema());
		documents.addAll(marshaller.getAttachments().values());
		return documents;
	}
}
