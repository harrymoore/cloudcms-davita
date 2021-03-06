# cloudcms-davita
Cloud CMS engagement artifacts for the Davita project

## install content model
    1. create a gitana.json file in this folder (project root)
    2. cd ./content-model
    3. set a branch id in deploy-content-model.sh (you can use "master" as the target branch id)
    4. run the shell script: ./deploy-content-model.sh

## install ui-module
    This Module implements the custom link generator "davita-link-generator" to display a link that can be copied 
    by the user to paste into emails.

    From Manage Platform / Modules register and deploy a new module:
    ID: davita-ui
    Title: davita-ui
    Type: github
    URL: https://github.com/harrymoore/cloudcms-davita.git
    Path: /ui-modules/ui
    Branch: master

## Running local keycloak server:
    cd /Users/harry/Downloads/keycloak-11.0.2/bin
    ./standalone.sh -Djboss.socket.binding.port-offset=100
    
    Open the ui at http://localhost:8180

## Cloud CMS Setup:
    1. Create editorial and live projects
        PCOMM       
            Curent PCOMM project: https://pixit.cloudcms.net/#/projects/344ae884f76f0ea75b5a
        PCOMM-LIVE
            Current PCOMM-LIVE project: https://pixit.cloudcms.net/#/projects/863848018d2a4f026200
    2. Create an "Application" object on each project
        This generates API keys
    3. Copy API keys to this folder as files:
        gitana-davita-pcomm.json
        gitana-davita-pcomm-live.json
    4. Install the content model into both projects (see above for details)
        cd into ./content-model
        ./deploy-content-model.sh
    5. Before the content model can be used, install the ui-module (see above for details)
        You may have to change the "Visibility" setting of this github project to "Public" before deploying the module.
        Don't forget to change it back to "Private" immediately as there are oAuth security credentials stored in the Java app's properties files
    8. Configure deployment target for publishing from editorial project (PCOMM) to live project (PCOMM-LIVE)
        Go to your plaftorm / "Manage Platform" / "Deployment Targets"
        Click "Create a Deployment Target"
            Enter:
                Title "PCOMM-LIVE master branch"
                "Deployment Target Type": "Branch"
            Click Next
                Select Branch "PCOMM-LIVE" and then "master"
            Click Pick
            Click Create
    6. Enable Publishing
        Go to "Manage Project" / Publishing
            Check "Enable Publishing for this Project"
            Check "Show Preview Buttons" and "Show Workflow Buttons"
        Go to "Manage Project" / Publications
            Click "Add a Publication"
            Select "Branch" "master"
            Select "State" "live"
            Select "Operation" "Deploy"
            Select "Deployment Target" "PCOMM-LIVE master branch" (created in a previous step)
            Click "Add Publication"
        Configure a similar Publication for the "Archived" operation
    7. Define a Preview Server on "PCOMM"
        This step is not required but it is a nice feature for editors to be able to click a button and immediately view their publishded content in the live application.
        The content needs to be "Published" first or the application will not be able to load the link.
        While in the project "PCOMM" (this step is not necessary for the "PCOMM-LIVE" project)
        Go to "Manage Project" / "Preview Servers"
        Check "Production" and set "Preview URL" to [SERVER_URL]/documents/{{document.id}}?clearCache=true
            where [SERVER_URL] is the url or the java application's run-time url. ex.: http://davita.ddns.net/documents/{{document.id}}?clearCache=true
    8. Define a custom index on "PCOMM-LIVE"
        Custom index will optimize queries. This custom index was designed to work with the queries made by the java application.
        While in the project "PCOMM-LIVE" (this step is not necessary for the "PCOMM" project)
        Go to "Manage Project" / Indexes
        Click "Create Custom Index"
        Enther a name. Something like "query-support"
        Copy and paste the JSON from ./indexes/enitlement.json into JSON text block
        Click "Create"
    9. Enable _statistics for a:has_tag so allow filtering out of unused tags in the ui. Only needed on the "PCOMM-LIVE" project
        Documentation for the statistics feature can be found here: https://www.cloudcms.com/documentation/api/statistics.html
        Click "Content Model" while in the "PCOMM-LIVE" project
        Click "Associations"
        Click "Show System Definitions"
        Click "a:has_tag"
        Click "Has Tag" in the content area to the right
        Click the "JSON" tab
        Add the folowing property to the "Has Tag" definition JSON:
            "mandatoryFeatures": {
                "f:statistics": {
            }
        The complete JSON definition should now look something like this:
            {
                "_parent": "a:linked",
                "type": "object",
                "title": "Has Tag",
                "description": "A relationship identifying that a source node has a target node tag",
                "systemBootstrapped": true,
                "properties": {},
                "mandatoryFeatures": {
                    "f:statistics": {
                    }
                },
                "$schema": "http://json-schema.org/draft-04/schema#"
            }
        Click "Save"
        Usage statistics for tagged content will now be maintained by Cloud CMS; Allowing queries such as this:
            {
                "_type": "n:tag",
                "_statistics": {
                    "a:has_tag_INCOMING": {
                        "$gt": 0
                    }
                }
            }

## Java App Setup:
    1. Configure java application credentials. Copy the corresponding properties from ./gitana-davita-pcomm-live.json to ./cloudcms-java-server/gitana.properties
        gitana.clientKey
        gitana.clientSecret
        gitana.username
        gitana.password
        gitana.baseURL
        gitana.application
        gitana.branch=master
    2. Configure java application properties in ./cloudcms-java-server/application.properties
        keycloak.enabled
            true or false
            enable or disable authentication and role checking against keycloak
        keycloak.realm
            keycloak realm name (case sensitive)
        keycloak.resource
            the keycloak realm's client id for this application
        keycloak.auth-server-url
            redirect url for keycloak auth service. something like this: http://localhost:8180/auth/
        keycloak.public-client=true
            prevents application from sending credentials. see https://www.keycloak.org/docs/4.8/securing_apps/
        keycloak.use-resource-role-mappings=true
            check roles against keycloak realm's client role definitions (rather than global realm defined roles)
        keycloak.principal-attribute=preferred_username
            use the user's name instead of id (guid-like value). used just for logging purposes
        keycloak.securityConstraints[0].authRoles[0]=user
        keycloak.securityConstraints[0].securityCollections[0].patterns[0]=/documents/*

        keycloak-logout.enabled=false
            the keycloak-logout.enabled property determines if a "Logout" button will be available to the end user which will     termintate their Keycloak session. Note the naming difference with other keycloak properties "keycloak-logout" vs "keycloak". This is because this property does not affect the keycloak authentication functions. It is only evaluated
            by the Thymleaf home page template cloudcms-java-server/src/main/resources/templates/index.html

        eureka.client.enabled=false
            Enable or disable Neflix Eureka discover service client
        eureka.client.serviceUrl.defaultZone
            This property is required by Netflix Eureka discovery service.
            Other Eureka configuration properties may be added as necessary

        pendo.enabled
            true or false
            enable or disable Pendo analytics JavaScript in templated html pages
        pendo.apiKey
            Pendo assigned api key for your Pendo account

        #spring.cache.type=NONE
            Caching is enabled by default. Uncomment the line above to disable it. Only do this for testing purposes. Caching MUST be used in production.

    3. Run the application. See ./cloudcms-java-server/README.md for details
        cd cloudcms-java-server
        mvn clean package -DskipTests spring-boot:run

## Keycloak / Cloud CMS external (physician) role definitions.
    Editorial users have the ability to select which roles a content item (and any of its associated documents) is available to.
    The names of the roles are defined in Keycloak and mirrored in Cloud CMS. If the role names change in Keycloak they must be
    correspondingly changed in Cloud CMS.

### Location of the role names in Cloud CMS
    Role names are stored in the content model type definiton JSON file:
        ./content-model/definitions/davita__document/node.json
    Role name display labels are stored in the type definiton's corresponding form definition JSON file:
        ./content-model/definitions/davita__document/forms/master.json

    In the section titled "entitlements" there is an "enum" property which lists the roles available for selection for a given content item.
    These are the exact values used for matching against a keycloak authorized user's list of roles. As such, they must match the keycloak
    role names exactly.
    ./content-model/definitions/davita__document/node.json:

        "entitlements": {
            "type": "array",
            "title": "Make Available to these Davita Roles",
            "items": {
                "type": "string",
                "enum": [
                    "user",
                    "DaVita Attending",
                    "DaVita Attending and JV Partner",
                    "DaVita Medical Director",
                    "DaVita Medical Director and JV Partner"
                ]
            }
        },

    In the form JSON, change the "optionLabels" names as needed to match the "enum" values in the definition JSON.
    These are the values that editorial users will see when filling out a form so the labels can be whatever makes the most sense for Davita users. For example the keycloak role named "users" may be ambiguous for editorial users so the label they will see is "Everyone".
    ./content-model/definitions/davita__document/forms/master.json

        "entitlements": {
            "order": 5,
            "type": "checkbox",
            "required": false,
            "multiple": true,
            "removeDefaultNone": true,
            "sort": false,
            "optionLabels": [
                "Everyone",
                "DaVita Attending",
                "DaVita Attending and JV Partner",
                "DaVita Medical Director",
                "DaVita Medical Director and JV Partner"
            ]        
		},

    Change the list of role name enum and optionLabels as needed and redeploy the content model.
    Note: any records that were created prior to updating the content model will need to be updated manually.
