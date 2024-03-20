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

Don't forget to build your js:
```
npx shadow-cljs compile app
```

If wanting to check dependencies, use: https://github.com/liquidz/antq
