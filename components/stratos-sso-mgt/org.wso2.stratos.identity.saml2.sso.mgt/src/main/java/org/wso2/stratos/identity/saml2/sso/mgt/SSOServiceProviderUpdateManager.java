/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.stratos.identity.saml2.sso.mgt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;
import org.wso2.carbon.identity.sso.saml.SSOServiceProviderConfigManager;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.stratos.identity.saml2.sso.mgt.internal.StratosSSOMgtServiceComponent;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;

/**
 * This class reads the Service Providers info from sso-idp-config.xml and add them to the
 * in-memory service provider map exposed by org.wso2.carbon.identity.sso.saml.
 * SSOServiceProviderConfigManager class.
 */
public class SSOServiceProviderUpdateManager {
    private static Log log = LogFactory.getLog(SSOServiceProviderUpdateManager.class);

    /**
     * Read the service providers from file, create SAMLSSOServiceProviderDO beans and add them
     * to the service providers map.
     */
    public void addServiceProviders(){
        SAMLSSOServiceProviderDO[] serviceProviders = readServiceProvidersFromFile();
        if(serviceProviders != null){
            SSOServiceProviderConfigManager configManager = StratosSSOMgtServiceComponent.
                    getSSOServiceProviderConfigManager();
            for(SAMLSSOServiceProviderDO spDO : serviceProviders){
                configManager.addServiceProvider(spDO.getIssuer(), spDO);
                log.info("A SSO Service Provider is registered for : " + spDO.getIssuer());
            }
        }
    }

    /**
     * Read the SP info from the sso-idp-config.xml and create an array of SAMLSSOServiceProviderDO
     * beans
     * @return An array of SAMLSSOServiceProviderDO beans
     */
    private SAMLSSOServiceProviderDO[] readServiceProvidersFromFile(){
        Document document = null;
        try {
            String configFilePath = CarbonUtils.getCarbonConfigDirPath() + File.separator + "sso-idp-config.xml";

            if(!isFileExisting(configFilePath)){
                log.warn("sso-idp-config.xml does not exist in the 'conf' directory. The system may" +
                         "depend on the service providers added through the UI.");
                return null;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(configFilePath);
        } catch (Exception e) {
            log.error("Error reading Service Providers from sso-idp-config.xml", e);
            return null;
        }

        Element element = document.getDocumentElement();
        NodeList nodeSet = element.getElementsByTagName(SSOMgtConstants.SERVICE_PROVIDER);
        SAMLSSOServiceProviderDO[] serviceProviders = new SAMLSSOServiceProviderDO[nodeSet.getLength()];
        boolean fullQualifyUserName = true;
        boolean singleLogout = true;
        boolean signAssertion = true;

        for (int i = 0; i < nodeSet.getLength(); i++) {
            Element elem = (Element) nodeSet.item(i);
            SAMLSSOServiceProviderDO spDO = new SAMLSSOServiceProviderDO();
            spDO.setIssuer(getTextValue(elem, SSOMgtConstants.ISSUER));
            spDO.setAssertionConsumerUrl(getTextValue(elem, SSOMgtConstants.ASSERTION_CONSUMER_URL));
            spDO.setLoginPageURL(getTextValue(elem, SSOMgtConstants.CUSTOM_LOGIN_PAGE));
            if((getTextValue(elem , SSOMgtConstants.USE_FULLY_QUALIFY_USER_NAME)) != null){
               fullQualifyUserName = Boolean.valueOf(getTextValue(elem , SSOMgtConstants.USE_FULLY_QUALIFY_USER_NAME));
            }
            if((getTextValue(elem , SSOMgtConstants.SINGLE_LOGOUT)) != null){
                singleLogout = Boolean.valueOf(getTextValue(elem , SSOMgtConstants.SINGLE_LOGOUT));
                spDO.setLogoutURL(getTextValue(elem, SSOMgtConstants.LOGOUT_URL));
            }
            if((getTextValue(elem , SSOMgtConstants.SIGN_ASSERTION)) != null){
                signAssertion = Boolean.valueOf(getTextValue(elem , SSOMgtConstants.SIGN_ASSERTION));
            }
            if(Boolean.valueOf(getTextValue(elem, SSOMgtConstants.SIG_VALIDATION))){
               spDO.setCertAlias(getTextValue(elem, SSOMgtConstants.CERT_ALIAS));
            }
            if(Boolean.valueOf(getTextValue(elem, SSOMgtConstants.ATTRIBUTE_PROFILE))){
                if(elem.getElementsByTagName(SSOMgtConstants.CLAIMS) != null){
               spDO.setRequestedClaims(getTextValueList(elem, SSOMgtConstants.CLAIM));
                }
                spDO.setEnableAttributesByDefault(Boolean.valueOf(getTextValue(elem, SSOMgtConstants.INCLUDE_ATTRIBUTE)));
            }
            if(Boolean.valueOf(getTextValue(elem, SSOMgtConstants.AUDIENCE_RESTRICTION))){
                if(elem.getElementsByTagName(SSOMgtConstants.AUDIENCE_LIST) != null){
                 spDO.setRequestedAudiences(getTextValueList(elem, SSOMgtConstants.AUDIENCE));
                }
            }

            spDO.setUseFullyQualifiedUsername(fullQualifyUserName);
            spDO.setDoSingleLogout(singleLogout);
            spDO.setDoSignAssertions(signAssertion);
            spDO.setDoSignResponse(Boolean.valueOf(getTextValue(elem, SSOMgtConstants.SIGN_RESPONSE)));
            spDO.setIdPInitSSOEnabled(Boolean.valueOf(getTextValue(elem, SSOMgtConstants.IDP_INIT)));
            spDO.setAttributeConsumingServiceIndex(getTextValue(elem, SSOMgtConstants.CONSUMING_SERVICE_INDEX));
            serviceProviders[i] = spDO;
        }
        return serviceProviders;
    }

    /**
     * Read the element value for the given element
     * @param element   Parent element
     * @param tagName   name of the child element
     * @return value of the element
     */
    private String getTextValue(Element element, String tagName) {
		String textVal = null;
		NodeList nl = element.getElementsByTagName(tagName);
		if(nl != null && nl.getLength() > 0) {
			Element el = (Element)nl.item(0);
			textVal = el.getFirstChild().getNodeValue();
		}
		return textVal;
	}

    private ArrayList<String> getTextValueList(Element element, String tagName) {
        ArrayList<String> textValList = new ArrayList<String>();
        NodeList nl = element.getElementsByTagName(tagName);
        if (nl != null && nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {
                Element el = (Element) nl.item(i);
                textValList.add(el.getFirstChild().getNodeValue());
            }
        }
        return textValList;
    }

    /**
     * Check whether a given file exists in the system
     * @param path file path
     * @return true, if file exists. False otherwise
     */
    private boolean isFileExisting(String path){
        File file = new File(path);
        if(file.exists()){
            return true;
        }
        return false;
    }


}
