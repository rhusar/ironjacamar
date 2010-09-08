/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.jca.common.metadata.resourceadapter;

import org.jboss.jca.common.api.metadata.common.AdminObject;
import org.jboss.jca.common.api.metadata.common.ConnectionDefinition;
import org.jboss.jca.common.api.metadata.common.TransactionSupportEnum;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapter;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapters;
import org.jboss.jca.common.metadata.MetadataParser;
import org.jboss.jca.common.metadata.ParserException;
import org.jboss.jca.common.metadata.common.CommonIronJacamarParser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 *
 * A ResourceAdapterParserr.
 *
 * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
 *
 */
public class ResourceAdapterParser extends CommonIronJacamarParser implements MetadataParser<ResourceAdapters>
{

   @Override
   public ResourceAdapters parse(InputStream xmlInputStream) throws Exception
   {

      XMLStreamReader reader = null;
      ResourceAdapters adapters = null;

      try
      {
         XMLInputFactory inputFactory = XMLInputFactory.newInstance();
         reader = inputFactory.createXMLStreamReader(xmlInputStream);

         //iterate over tags
         int iterate;
         try
         {
            iterate = reader.nextTag();
         }
         catch (XMLStreamException e)
         {
            //founding a non tag..go on. Normally non-tag found at beginning are comments or DTD declaration
            iterate = reader.nextTag();
         }
         switch (iterate)
         {
            case END_ELEMENT : {
               // should mean we're done, so ignore it.
               break;
            }
            case START_ELEMENT : {

               switch (Tag.forName(reader.getLocalName()))
               {
                  case RESOURCE_ADPTERS : {
                     adapters = parseResourceAdapters(reader);
                     break;
                  }
                  default :
                     throw new ParserException("Unexpected element:" + reader.getLocalName());
               }

               break;
            }
            default :
               throw new IllegalStateException();
         }
      }
      finally
      {
         if (reader != null)
            reader.close();
      }
      return adapters;

   }

   private ResourceAdapters parseResourceAdapters(XMLStreamReader reader) throws XMLStreamException,
      ParserException
   {
      ArrayList<ResourceAdapter> resourceAdapters = new ArrayList<ResourceAdapter>();
      while (reader.hasNext())
      {
         switch (reader.nextTag())
         {
            case END_ELEMENT : {
               if (Tag.forName(reader.getLocalName()) == Tag.RESOURCE_ADPTERS)
               {
                  resourceAdapters.trimToSize();
                  return new ResourceAdaptersImpl(resourceAdapters);
               }
               else
               {
                  if (ResourceAdapters.Tag.forName(reader.getLocalName()) == ResourceAdapters.Tag.UNKNOWN)
                  {
                     throw new ParserException("unexpected end tag" + reader.getLocalName());
                  }
               }
               break;
            }
            case START_ELEMENT : {
               switch (ResourceAdapters.Tag.forName(reader.getLocalName()))
               {
                  case RESOURCE_ADPTER : {
                     resourceAdapters.add(parseResourceAdapter(reader));
                     break;
                  }
                  default :
                     throw new ParserException("Unexpected element:" + reader.getLocalName());
               }
               break;
            }
         }
      }
      throw new ParserException("Reached end of xml document unexpectedly");
   }

   private ResourceAdapter parseResourceAdapter(XMLStreamReader reader) throws XMLStreamException, ParserException
   {
      ArrayList<ConnectionDefinition> connectionDefinitions = new ArrayList<ConnectionDefinition>();
      ArrayList<AdminObject> adminObjects = new ArrayList<AdminObject>();
      ArrayList<String> beanValidationGroups = new ArrayList<String>();
      String bootstrapContext = null;
      String archive = null;
      TransactionSupportEnum transactionSupport = null;
      HashMap<String, String> configProperties = new HashMap<String, String>();
      while (reader.hasNext())
      {
         switch (reader.nextTag())
         {
            case END_ELEMENT : {
               if (ResourceAdapters.Tag.forName(reader.getLocalName()) == ResourceAdapters.Tag.RESOURCE_ADPTER)
               {
                  return new ResourceAdapterImpl(archive, transactionSupport, connectionDefinitions, adminObjects,
                                                 configProperties, beanValidationGroups, bootstrapContext);
               }
               else
               {
                  if (ResourceAdapter.Tag.forName(reader.getLocalName()) == ResourceAdapter.Tag.UNKNOWN)
                  {
                     throw new ParserException("unexpected end tag" + reader.getLocalName());
                  }
               }
               break;
            }
            case START_ELEMENT : {
               switch (ResourceAdapter.Tag.forName(reader.getLocalName()))
               {
                  case ADMIN_OBJECTS :
                  case CONNECTION_DEFINITIONS :
                  case BEAN_VALIDATION_GROUPS : {
                     //ignore it,we will parse bean-validation-group,admin_object and connection_definition directly
                     break;
                  }
                  case ADMIN_OBJECT : {
                     adminObjects.add(parseAdminObjects(reader));
                     break;
                  }

                  case CONNECTION_DEFINITION : {
                     connectionDefinitions.add(parseConnectionDefinitions(reader));
                     break;
                  }
                  case BEAN_VALIDATION_GROUP : {
                     beanValidationGroups.add(elementAsString(reader));
                     break;
                  }
                  case BOOTSTRAP_CONTEXT : {
                     bootstrapContext = elementAsString(reader);
                     break;
                  }
                  case CONFIG_PROPERTY : {
                     configProperties.put(attributeAsString(reader, "name"), elementAsString(reader));
                     break;

                  }
                  case TRANSACTION_SUPPORT : {
                     transactionSupport = TransactionSupportEnum.valueOf(elementAsString(reader));
                     break;
                  }
                  case ARCHIVE : {
                     archive = elementAsString(reader);
                     break;
                  }
                  default :
                     throw new ParserException("Unexpected element:" + reader.getLocalName());
               }
               break;
            }
         }
      }
      throw new ParserException("Reached end of xml document unexpectedly");
   }

   /**
   *
   * A Tag.
   *
   * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
   *
   */
   public enum Tag
   {
      /** always first
       *
       */
      UNKNOWN(null),

      /** jboss-ra tag name
       *
       */
      RESOURCE_ADPTERS("resource-adapters");

      private final String name;

      /**
       *
       * Create a new Tag.
       *
       * @param name a name
       */
      Tag(final String name)
      {
         this.name = name;
      }

      /**
       * Get the local name of this element.
       *
       * @return the local name
       */
      public String getLocalName()
      {
         return name;
      }

      private static final Map<String, Tag> MAP;

      static
      {
         final Map<String, Tag> map = new HashMap<String, Tag>();
         for (Tag element : values())
         {
            final String name = element.getLocalName();
            if (name != null)
               map.put(name, element);
         }
         MAP = map;
      }

      /**
      *
      * Static method to get enum instance given localName string
      *
      * @param localName a string used as localname (typically tag name as defined in xsd)
      * @return the enum instance
      */
      public static Tag forName(String localName)
      {
         final Tag element = MAP.get(localName);
         return element == null ? UNKNOWN : element;
      }

   }

}