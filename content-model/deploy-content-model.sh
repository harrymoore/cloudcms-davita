#!/bin/sh

# build form fields from definition
# npx cloudcms-util create-form-fields --data-path ./ --qname davita:document --overwrite

# use cloudcms-util import feature to upload a local copy of the content model to a cloud cms branch
# npm install cloudcms-util

# harry's dev projects:
npx cloudcms-util import -g ../gitana-editorial.json --branch master --all-definitions --folder-path .
npx cloudcms-util import -g ../gitana-live.json --branch master --all-definitions --folder-path .

# Davita / pixit projects:
npx cloudcms-util import -g ../gitana-davita-pcomm.json --branch master --all-definitions --folder-path .
npx cloudcms-util import -g ../gitana-davita-pcomm-live.json --branch master --all-definitions --folder-path .
