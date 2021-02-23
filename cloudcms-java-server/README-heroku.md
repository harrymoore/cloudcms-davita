## Deploy to heroku
git subtree push --prefix cloudcms-java-server heroku master

heroku logs --tail --num=500
