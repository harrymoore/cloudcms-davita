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
