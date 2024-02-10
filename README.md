This branch serves as a template to make your own web app, with authenticated pages.

First, ensure you're on the correct branch, and delete the .git:
```
git checkout template
rm -rf .git
```

Next name your project:
```
./name-project.sh
```

Tidy up and make your new git repo:
```
rm name-project.sh
git init
git add .
git commit -m
```

Create a PostgreSQL database [like this](http://readtheorg.xylon.me.uk/local_postgres.html#org12d8c14).

Then, make a `env/dev/resources/db-credentials.edn` of the form:
```
{:db-name "my_app"
 :user "my_app"
 :password "password"}
```

Now migrate your database. Fire up repl and run this in your `user` namespace:
```
(go)
(migrate)
```

Now add your dev admin user:
```
(query (q/create-user "testuser" "testpass"))
```
