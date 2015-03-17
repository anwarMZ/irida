package ca.corefacility.bioinformatics.irida.web.assembler.resource;

import ca.corefacility.bioinformatics.irida.model.IridaResourceSupport;
import ca.corefacility.bioinformatics.irida.model.IridaThing;
import ca.corefacility.bioinformatics.irida.model.joins.Join;

import javax.xml.bind.annotation.XmlElement;

/**
 * An implementation of a resource that only has a label and an identifier.
 * 
 */
public class LabelledRelationshipResource<Owner extends IridaThing, Child extends IridaThing> extends
		IridaResourceSupport {
	@XmlElement
	private String label;

	private Join<Owner, Child> resource;

	public LabelledRelationshipResource(String label, Join<Owner, Child> r) {
		this.resource = r;
		this.label = label;
	}

	public String getLabel() {
		return this.label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * We don't want to expose the {@link Identifier} for the
	 * {@link Relationship}, but rather the {@link Identifier} for the object of
	 * the {@link Relationship}.
	 * 
	 * @return the {@link Identifier} for the object of the relationship.
	 */
	public String getIdentifier() {
		return resource.getObject().getId().toString();
	}
	
	public Join<Owner, Child> getResource() {
		return resource;
	}
}
