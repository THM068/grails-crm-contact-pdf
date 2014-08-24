/*
 * Copyright (c) 2014 Goran Ehrsson.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grails.plugins.crm.contact

import grails.events.Listener

/**
 * Print service for contacts.
 */
class CrmContactPdfPrintService {

    static transactional = false

    def crmSecurityService
    def crmThemeService
    def selectionService
    def pdfRenderingService
    def grailsLinkGenerator

    private File getLogoFile(Long tenantId) {
        crmThemeService.getLogoFile(tenantId, 'large') ?: crmThemeService.getLogoFile(tenantId, 'medium')
    }

    @Listener(namespace = "crmContact", topic = "exportLayout")
    def exportLayout(data) {
        return [name: 'Adresslista', description: 'Enkel adresslista i stående A4 format',
                thumbnail: grailsLinkGenerator.resource(dir: 'images', file: 'contact-list-thumbnail.png'),
                contentType: 'application/pdf', namespace: 'crmContact', topic: 'print-contact-list']
    }

    @Listener(namespace = "crmContact", topic = "print-contact-list")
    def print(params) {
        if (!params.tenant) {
            log.error("Parameter [tenant] missing in event [print-contact-list]: $params")
            throw new IllegalArgumentException("Mandatory parameter [tenant] was not specified in the event")
        }
        if (!params.sort) {
            params.sort = 'name'
        }
        if (!params.order) {
            params.order = 'asc'
        }
        params.offset = 0
        params.remove('max')

        def user = params.user
        def tempFile
        crmSecurityService.runAs(user.username, params.tenant) {
            def baseURI = new URI('bean://crmContactService/list')
            def query = params.getSelectionQuery()
            def uri = params.getSelectionURI() ?: selectionService.addQuery(baseURI, query)
            def result = selectionService.select(uri, params)

            if (result) {
                def logo = getLogoFile(params.tenant)

                tempFile = File.createTempFile("crm", ".pdf")
                tempFile.deleteOnExit()

                // Render to a file
                tempFile.withOutputStream { outputStream ->
                    pdfRenderingService.render(template: "simple", plugin: "crm-contact-pdf", controller: "crmContact",
                            model: [crmContactList: result, crmContactTotal: result.totalCount, selection: uri, user: user, logo: logo.bytes],
                            outputStream)
                    outputStream.flush()
                }
            }
        }
        [file: tempFile, filename: 'Kontakter', contentType: 'application/pdf']
    }
}
