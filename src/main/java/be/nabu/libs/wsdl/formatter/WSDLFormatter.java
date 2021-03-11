package be.nabu.libs.wsdl.formatter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import be.nabu.libs.wsdl.api.Binding;
import be.nabu.libs.wsdl.api.BindingOperation;
import be.nabu.libs.wsdl.api.Fragment;
import be.nabu.libs.wsdl.api.Message;
import be.nabu.libs.wsdl.api.MessagePart;
import be.nabu.libs.wsdl.api.Operation;
import be.nabu.libs.wsdl.api.PortType;
import be.nabu.libs.wsdl.api.Service;
import be.nabu.libs.wsdl.api.ServicePort;
import be.nabu.libs.wsdl.api.WSDLDefinition;
import be.nabu.utils.xml.XMLUtils;

/**
 * Imports currently have to be marshalled separately although the import statements are generated
 */
public class WSDLFormatter {
	
	public static final String NAMESPACE = "http://schemas.xmlsoap.org/wsdl/";
	public static final String SOAP_HTTP_NAMESPACE = "http://schemas.xmlsoap.org/soap/http";
	public static final String SOAP_NAMESPACE_11 = "http://schemas.xmlsoap.org/wsdl/soap/";
	public static final String SOAP_NAMESPACE_12 = "http://schemas.xmlsoap.org/wsdl/soap12/";
	public static final List<String> SOAP_NAMESPACES = Arrays.asList(SOAP_NAMESPACE_11, SOAP_NAMESPACE_12);
	
	private boolean elementQualified, attributeQualified;
	private boolean hidePrivatelyScoped;
	
	/**
	 * Contains the namespace > prefix mapping based on the imports
	 */
	private Map<String, String> namespaces = new HashMap<String, String>();
	private Document document;
			
	public Document format(WSDLDefinition definition) {
		document = XMLUtils.newDocument(true);
		Element definitions = document.createElementNS(NAMESPACE, "wsdl:definitions");
		document.appendChild(definitions);
		definitions.setAttribute("xmlns:wsdl", NAMESPACE);
		definitions.setAttribute("xmlns:tns", definition.getTargetNamespace());
		definitions.setAttribute("xmlns:soap", definition.getSoapVersion() != null && definition.getSoapVersion() == 1.2 ? SOAP_NAMESPACE_12 : SOAP_NAMESPACE_11);
		definitions.setAttribute("targetNamespace", definition.getTargetNamespace());
		namespaces.put(definition.getTargetNamespace(), "tns");
		formatImports(definitions, definition.getImports());
		TypeRegistryFormatter typeFormatter = new TypeRegistryFormatter();
		typeFormatter.setAttributeQualified(attributeQualified);
		typeFormatter.setElementQualified(elementQualified);
		typeFormatter.setHidePrivatelyScoped(hidePrivatelyScoped);
		List<Document> typeDocuments = typeFormatter.format(definition.getRegistry());
		Element types = document.createElementNS(NAMESPACE, "wsdl:types");
		definitions.appendChild(types);
		for (Document typeDocument : typeDocuments) {
			types.appendChild(document.importNode(typeDocument.getDocumentElement(), true));
		}
		formatMessages(definitions, definition.getMessages());
		formatPortTypes(definitions, definition.getPortTypes());
		formatBindings(definitions, definition.getBindings());
		formatServices(definitions, definition.getServices());
		return document;
	}
	
	private void formatImports(Element parent, List<WSDLDefinition> imports) {
		if (imports != null) {
			for (WSDLDefinition imported : imports) {
				Element element = parent.getOwnerDocument().createElementNS(NAMESPACE, "wsdl:import");
				element.setAttribute("namespace", imported.getTargetNamespace());
//				element.setAttribute("location", "attachments:/" + imported.getTargetNamespace());
				parent.appendChild(element);
			}
		}
	}
	
	private void formatMessages(Element parent, List<Message> messages) {
		if (messages != null) {
			for (Message message: messages) {
				Element element = parent.getOwnerDocument().createElementNS(NAMESPACE, "wsdl:message");
				element.setAttribute("name", message.getName());
				formatParts(element, message.getParts());
				parent.appendChild(element);
			}
		}
	}
	
	private void formatParts(Element parent, List<MessagePart> parts) {
		if (parts != null) {
			for (MessagePart part : parts) {
				Element element = parent.getOwnerDocument().createElementNS(NAMESPACE, "wsdl:part");
				element.setAttribute("name", part.getName());
				if (part.getElement() != null) {
					element.setAttribute("element", getPrefix(part.getElement().getNamespace()) + part.getElement().getName());
				}
				else if (part.getType() != null) {
					element.setAttribute("type", getPrefix(part.getType().getNamespace()) + part.getType().getName());
				}
				parent.appendChild(element);
			}
		}
	}
	
	private void formatPortTypes(Element parent, List<PortType> portTypes) {
		if (portTypes != null) {
			for (PortType portType : portTypes) {
				Element element = parent.getOwnerDocument().createElementNS(NAMESPACE, "wsdl:portType");
				element.setAttribute("name", portType.getName());
				formatOperations(element, portType.getOperations());
				parent.appendChild(element);
			}
		}
	}
	
	private void formatOperations(Element parent, List<Operation> operations) {
		if (operations != null) {
			for (Operation operation : operations) {
				Element element = parent.getOwnerDocument().createElementNS(NAMESPACE, "wsdl:operation");
				element.setAttribute("name", operation.getName());
				if (operation.getInput() != null) {
					Element input = parent.getOwnerDocument().createElementNS(NAMESPACE, "wsdl:input");
					input.setAttribute("name", operation.getInput().getName());
					input.setAttribute("message", getPrefix(operation.getInput().getDefinition().getTargetNamespace()) + operation.getInput().getName());
					element.appendChild(input);
				}
				if (operation.getOutput() != null) {
					Element output = parent.getOwnerDocument().createElementNS(NAMESPACE, "wsdl:output");
					output.setAttribute("name", operation.getOutput().getName());
					output.setAttribute("message", getPrefix(operation.getOutput().getDefinition().getTargetNamespace()) + operation.getOutput().getName());
					element.appendChild(output);
				}
				if (operation.getFaults() != null) {
					for (Message fault : operation.getFaults()) {
						Element faultElement = parent.getOwnerDocument().createElementNS(NAMESPACE, "wsdl:fault");
						faultElement.setAttribute("name", fault.getName());
						faultElement.setAttribute("message", getPrefix(fault.getDefinition().getTargetNamespace()) + fault.getName());
						element.appendChild(faultElement);
					}
				}
				parent.appendChild(element);
			}
		}
	}
	
	private void formatBindings(Element parent, List<Binding> bindings) {
		if (bindings != null) {
			for (Binding binding : bindings) {
				String soapNamespace = getSoapNamespace(binding);
				Element element = parent.getOwnerDocument().createElementNS(NAMESPACE, "wsdl:binding");
				element.setAttribute("name", binding.getName());
				element.setAttribute("type", getPrefix(binding.getPortType().getDefinition().getTargetNamespace()) + binding.getPortType().getName());
				Element childBinding;
				if (SOAP_NAMESPACES.contains(binding.getTransport().getNamespace())) {
					childBinding = parent.getOwnerDocument().createElementNS(soapNamespace, "soap:binding");
				}
				else {
					childBinding = parent.getOwnerDocument().createElementNS(binding.getTransport().getNamespace(), "transport:binding");
					childBinding.setAttribute("xmlns:transport", binding.getTransport().getNamespace());
				}
				childBinding.setAttribute("transport", binding.getTransport().getTransport());
				element.appendChild(childBinding);
				formatBindingOperations(element, binding.getOperations());
				parent.appendChild(element);

			}
		}
	}
	
	private String getSoapNamespace(Fragment fragment) {
		return fragment.getDefinition().getSoapVersion() != null && fragment.getDefinition().getSoapVersion() == 1.2 ? SOAP_NAMESPACE_12 : SOAP_NAMESPACE_11;
	}
	
	private void formatBindingOperations(Element parent, List<BindingOperation> operations) {
		if (operations != null) {
			for (BindingOperation operation : operations) {
				String soapNamespace = getSoapNamespace(operation);
				Element element = parent.getOwnerDocument().createElementNS(NAMESPACE, "wsdl:operation");
				element.setAttribute("name", operation.getName());
				Element childBinding = parent.getOwnerDocument().createElementNS(soapNamespace, "soap:operation");
				if (operation.getSoapAction() != null) {
					childBinding.setAttribute("soapAction", operation.getSoapAction());
				}
				if (operation.getStyle() != null) {
					childBinding.setAttribute("style", operation.getStyle().toString().toLowerCase());
				}
				element.appendChild(childBinding);
				if (operation.getOperation().getInput() != null) {
					Element inputElement = parent.getOwnerDocument().createElementNS(NAMESPACE, "wsdl:input");
					inputElement.setAttribute("name", operation.getOperation().getInput().getName());
					Element body = parent.getOwnerDocument().createElementNS(soapNamespace, "soap:body");
					if (operation.getUse() != null) {
						body.setAttribute("use", operation.getUse().toString().toLowerCase());
					}
					inputElement.appendChild(body);
					element.appendChild(inputElement);
				}
				if (operation.getOperation().getOutput() != null) {
					Element outputElement = parent.getOwnerDocument().createElementNS(NAMESPACE, "wsdl:output");
					outputElement.setAttribute("name", operation.getOperation().getOutput().getName());
					Element body = parent.getOwnerDocument().createElementNS(soapNamespace, "soap:body");
					if (operation.getUse() != null) {
						body.setAttribute("use", operation.getUse().toString().toLowerCase());
					}
					outputElement.appendChild(body);
					element.appendChild(outputElement);
				}
				if (operation.getOperation().getFaults() != null) {
					for (Message fault : operation.getOperation().getFaults()) {
						Element faultElement = parent.getOwnerDocument().createElementNS(NAMESPACE, "wsdl:fault");
						faultElement.setAttribute("name", fault.getName());
						Element body = parent.getOwnerDocument().createElementNS(soapNamespace, "soap:body");
						if (operation.getUse() != null) {
							body.setAttribute("use", operation.getUse().toString().toLowerCase());
						}
						faultElement.appendChild(body);
						element.appendChild(faultElement);
					}
				}
				parent.appendChild(element);
			}
		}
	}
	
	private void formatServices(Element parent, List<Service> services) {
		if (services != null) {
			for (Service service : services) {
				Element element = parent.getOwnerDocument().createElementNS(NAMESPACE, "wsdl:service");
				element.setAttribute("name", service.getName());
				formatPorts(element, service.getPorts());
				parent.appendChild(element);
			}
		}
	}
	
	private void formatPorts(Element parent, List<ServicePort> ports) {
		if (ports != null) {
			for (ServicePort port : ports) {
				String soapNamespace = getSoapNamespace(port);
				Element element = parent.getOwnerDocument().createElementNS(NAMESPACE, "wsdl:port");
				element.setAttribute("name", port.getName());
				element.setAttribute("binding", getPrefix(port.getBinding().getDefinition().getTargetNamespace()) + port.getBinding().getName());
				if (port.getEndpoint() != null) {
					Element address = parent.getOwnerDocument().createElementNS(soapNamespace, "soap:address");
					address.setAttribute("location", port.getEndpoint());
					element.appendChild(address);
				}
				parent.appendChild(element);
			}
		}
	}

	private String getPrefix(String namespace) {
		if (!namespaces.containsKey(namespace)) {
			String prefix = "tns" + namespaces.size();
			namespaces.put(namespace, prefix);
			document.getDocumentElement().setAttribute("xmlns:" + prefix, namespace);
		}
		return namespaces.get(namespace) == null ? "" : namespaces.get(namespace) + ":";
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
