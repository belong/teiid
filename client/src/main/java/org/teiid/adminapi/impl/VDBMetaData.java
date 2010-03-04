/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */
package org.teiid.adminapi.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.jboss.managed.api.annotation.ManagementComponent;
import org.jboss.managed.api.annotation.ManagementObject;
import org.jboss.managed.api.annotation.ManagementObjectID;
import org.jboss.managed.api.annotation.ManagementOperation;
import org.jboss.managed.api.annotation.ManagementProperty;
import org.teiid.adminapi.Model;
import org.teiid.adminapi.VDB;
import org.teiid.adminapi.impl.ModelMetaData.ValidationError;

import com.metamatrix.core.CoreConstants;

@ManagementObject(componentType=@ManagementComponent(type="teiid",subtype="vdb"))
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "", propOrder = {
    "description",
    "JAXBProperties",
    "JAXBModels",
    "securityRoleMappings"
})
@XmlRootElement(name = "vdb")
public class VDBMetaData extends AdminObjectImpl implements VDB {
	private static final String STATUS_KEY = "status"; //$NON-NLS-1$

	private static final long serialVersionUID = -4723595252013356436L;
	
	private ListOverMap<ModelMetaData> models = new ListOverMap<ModelMetaData>(new KeyBuilder<ModelMetaData>() {
		@Override
		public String getKey(ModelMetaData entry) {
			return entry.getName();
		}
	});
	
	@XmlAttribute(name = "version", required = true)
	private int version = 1;
	
	@XmlElement(name = "description")
	private String description;
	
    @XmlElement(name = "role-mapping")
    protected ListOverMap<ReferenceMappingMetadata> securityRoleMappings = new ListOverMap<ReferenceMappingMetadata>(new KeyBuilder<ReferenceMappingMetadata>() {
			@Override
			public String getKey(ReferenceMappingMetadata entry) {
				return entry.getRefName();
			}
	});
    
	private String fileUrl = null;
	
	public VDBMetaData() {
		// auto add sytem model.
		ModelMetaData system = new ModelMetaData();
		system.setName(CoreConstants.SYSTEM_MODEL);
		system.setVisible(true);
		system.setModelType(Model.Type.PHYSICAL.name());
		system.addSourceMapping("system", "system");
		system.setSupportsMultiSourceBindings(false);
				
		addModel(system);
	}

	@ManagementProperty(description="Name of the VDB", readOnly=true)
	@ManagementObjectID(type="vdb")
	@XmlAttribute(name = "name", required = true)
	public String getName() {
		return super.getName();
	}
	
	// This needed by JAXB marshaling
	public void setName(String name) {
		super.setName(name);
	} 
	
	@Override
	@ManagementProperty(description="VDB Status")
	public Status getStatus() {
		String status = getPropertyValue(STATUS_KEY);
		if (status != null) {
			return VDB.Status.valueOf(status);
		}
		return VDB.Status.ACTIVE;
	}
	
	public void setStatus(Status s) {
		addProperty(STATUS_KEY, s.name());
	}
	
	@Override
	@ManagementProperty(description="VDB version", readOnly=true)
	public int getVersion() {
		return this.version;
	}
	
	public void setVersion(int version) {
		this.version = version;
	}	
		
	@Override
	@ManagementProperty(description = "The VDB file url", readOnly=true)
	public String getUrl() {
		return this.fileUrl;
	}
	
	public void setUrl(String url) {
		this.fileUrl = url;
	}

	@ManagementProperty(description="Models in a VDB", managed=true)
	public List<ModelMetaData> getModels(){
		return new ArrayList<ModelMetaData>(this.models.getMap().values());
	}
	
	/**
	 * This simulating a list over a map. JAXB requires a list and performance recommends
	 * map and we would like to keep one variable to represent both. 
	 * @return
	 */
	@XmlElement(name = "model", required = true, type = ModelMetaData.class)
	protected List<ModelMetaData> getJAXBModels(){
		return models;
	}	
	
	public void addModel(ModelMetaData m) {
		this.models.getMap().put(m.getName(), m);
	}	

	@Override
	@ManagementProperty(description = "Description", readOnly=true)	
	public String getDescription() {
		return this.description;
	}
	
	public void setDescription(String desc) {
		this.description = desc;
	}

	@Override
	@ManagementProperty(description = "VDB validity errors", readOnly=true)		
	public List<String> getValidityErrors(){
		List<String> allErrors = new ArrayList<String>();
		for (ModelMetaData model:this.models.getMap().values()) {
			List<ValidationError> errors = model.getErrors();
			if (errors != null && !errors.isEmpty()) {
				for (ValidationError m:errors) {
					if (ValidationError.Severity.valueOf(m.getSeverity()).equals(ValidationError.Severity.ERROR)) {
						allErrors.add(m.getValue());
					}
				}
			}
		}
		return allErrors; 
	}

	@Override
	@ManagementProperty(description = "Is VDB Valid", readOnly=true)
    public boolean isValid() {
        if (!getValidityErrors().isEmpty()) {
            return false;
        }
                
        if (getModels().isEmpty()) {
            return false;        	
        }
    	for(ModelMetaData m: this.models.getMap().values()) {
    		if (m.isSource()) {
    			List<String> resourceNames = m.getSourceNames();
    			if (resourceNames.isEmpty()) {
    				return false;
    			}
    		}
    	}
        return true;
    } 	
    
	public String toString() {
		return getName()+"."+getVersion()+ models.getMap().values(); //$NON-NLS-1$
	}

	@ManagementOperation(description = "Get the model with given name")		
	public ModelMetaData getModel(String modelName) {
		return this.models.getMap().get(modelName);
	}
	
	public Set<String> getMultiSourceModelNames(){
		Set<String> list = new HashSet<String>();
		for(ModelMetaData m: models.getMap().values()) {
			if (m.isSupportsMultiSourceBindings()) {
				list.add(m.getName());
			}
		}
		return list;
	}
	
    @ManagementProperty(description="Security refrence mappings", managed=true)
	public List<ReferenceMappingMetadata> getSecurityRoleMappings() {
		return new ArrayList<ReferenceMappingMetadata>(this.securityRoleMappings);
	}    
	
	public void addSecurityRoleMapping(ReferenceMappingMetadata data) {
		this.securityRoleMappings.getMap().put(data.getRefName(), data);
	}
	
	// this one manages the Management API
	@Override
	@ManagementProperty(description = "Properties", readOnly=true)
    public Properties getProperties() {
        return super.getProperties();
    }		
	
	// This one manages the JAXB binding
	@Override
	@XmlElement(name = "property", type = PropertyMetadata.class)
	protected List<PropertyMetadata> getJAXBProperties(){
		return super.getJAXBProperties();
	}
}