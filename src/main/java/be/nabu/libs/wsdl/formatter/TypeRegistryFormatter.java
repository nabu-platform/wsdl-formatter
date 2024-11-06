/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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
	
	private boolean elementQualified, attributeQualified;
	private boolean hidePrivatelyScoped;
	
	public List<Document> format(TypeRegistry registry) {
		List<Document> documents = new ArrayList<Document>();
		XSDDefinitionMarshaller marshaller = new XSDDefinitionMarshaller();
		marshaller.setHidePrivatelyScoped(hidePrivatelyScoped);
		marshaller.setIncludeSchemaLocation(false);
		marshaller.setIsElementQualified(elementQualified);
		marshaller.setIsAttributeQualified(attributeQualified);
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
		if (marshaller.getSchema() != null) {
			documents.add(marshaller.getSchema());
		}
		documents.addAll(marshaller.getAttachments().values());
		return documents;
	}

	public boolean isElementQualified() {
		return elementQualified;
	}
	public void setElementQualified(boolean elementQualified) {
		this.elementQualified = elementQualified;
	}

	public boolean isAttributeQualified() {
		return attributeQualified;
	}
	public void setAttributeQualified(boolean attributeQualified) {
		this.attributeQualified = attributeQualified;
	}

	public boolean isHidePrivatelyScoped() {
		return hidePrivatelyScoped;
	}

	public void setHidePrivatelyScoped(boolean hidePrivatelyScoped) {
		this.hidePrivatelyScoped = hidePrivatelyScoped;
	}
}
