package org.hl7.fhir.dstu3.elementmodel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import org.hl7.fhir.dstu3.exceptions.FHIRException;
import org.hl7.fhir.dstu3.model.Base;
import org.hl7.fhir.dstu3.model.ElementDefinition;
import org.hl7.fhir.dstu3.model.PrimitiveType;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.model.StructureDefinition.StructureDefinitionKind;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.dstu3.utils.IWorkerContext;
import org.hl7.fhir.dstu3.utils.ProfileUtilities;

public class ObjectConverter  {

  private IWorkerContext context;

  public ObjectConverter(IWorkerContext context) {
    this.context = context;
  }

  public Element convert(Resource ig) throws Exception {
    ByteArrayOutputStream bs = new ByteArrayOutputStream();
    org.hl7.fhir.dstu3.formats.JsonParser jp = new org.hl7.fhir.dstu3.formats.JsonParser();
    jp.compose(bs, ig);
    ByteArrayInputStream bi = new ByteArrayInputStream(bs.toByteArray());
    return new JsonParser(context).parse(bi);
  }

  public Element convert(Property property, Type type) throws FHIRException {
    return convertElement(property, type);
  }
  
  private Element convertElement(Property property, Base base) throws FHIRException {
    if (base == null)
      return null;
    String tn = base.fhirType();
    StructureDefinition sd = context.fetchResource(StructureDefinition.class, "http://hl7.org/fhir/StructureDefinition/"+tn);
    if (sd == null)
      throw new FHIRException("Unable to find definition for type "+tn);
    Element res = new Element(property.getName(), property);
    if (sd.getKind() == StructureDefinitionKind.PRIMITIVETYPE) 
      res.setValue(((PrimitiveType) base).asStringValue());

    List<ElementDefinition> children = ProfileUtilities.getChildMap(sd, sd.getSnapshot().getElementFirstRep()); 
    for (ElementDefinition child : children) {
      String n = tail(child.getPath());
      if (sd.getKind() != StructureDefinitionKind.PRIMITIVETYPE || !"value".equals(n)) {
        Base[] values = base.getProperty(n.hashCode(), n, false);
        if (values != null)
          for (Base value : values) {
            res.getChildren().add(convertElement(new Property(context, child, sd), value));
          }
      }
    }
    return res;
  }

  private String tail(String path) {
    if (path.contains("."))
      return path.substring(path.lastIndexOf('.')+1);
    else
      return path;
  }


}
