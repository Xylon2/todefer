# ToDefer

ToDefer is a web app for managing tasks on a "best-effort" basis.

It's build with Clojure tools.build and shadow-cljs and requires PostgreSQL and
Redis.

## Manifesto

### What problem it seeks to solve

Todefer is built around the realization that most of the tasks I want to keep
track of are not actually things I want "ToDo", but rather things I want to put
off until later. Hence the name "ToDefer".

However, there is also a need for "TODO" functionality. But this should be
limited to just planing today or tomorrow, no further ahead.

So Todefer attempts to manage both deferred stuff and also provide good planning
for 1 day ahead.

### What ToDefer provides

ToDefer allows one to define named pages containing either "Tasks" or
"Habits". And also "ToDo" pages which aggregate stuff from one or more Tasks or
Habits pages.

On Tasks pages, one defers tasks either into named categories, or
to specific dates. The tasks then dissapear into their categories
until either one brings the out manually or they become due
(in-the-case of date categories). The due tasks are shown at the
top of the page.

On habits pages, each "Habit" has a recurrance frequency. Habits
that are not due are hidden away. Habits that are due are shown
at the top of the page to be dealt with.

Note that Habits recurr from the date they are marked done, not the
date they were due to be done.

ToDo pages simply show all tasks from all linked Task or Habit pages which have
been marked for today or tomorrow.

### What is the intended workflow?

Add tasks to it all day whennever you want.

Every evening or morning, go through your Tasks and Habits pages, and mark as
ToDo anything which you want to do today or tomorrow.

### What it's not

ToDefer is not:
- a calendar
- a project management system

## Running this code

Here I explain briefly how you may run this code on your workstation for development.

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

## Upgrading the codebase dependencies

Update deps.edn with: https://github.com/liquidz/antq

Update node with: `npm update`

## Deployment

This is an example guide on how to deploy this app onto Debian.

### Compile
Compile your js:
```
npx shadow-cljs release app
```

Compile your .jar:
```
clj -T:build uber
```

### Server
You need a Debian server with these packages installed from apt:
- postgresql
- redis
- default-jre
- nginx

### DB
Create a postgres user and database.

### Application
Create a system user "deploy".

Make these directories and chown them to deploy
- /var/todefer
- /var/log/todefer

Upload the .jar you compiled to /var/todefer.

Add a systemd config at `/lib/systemd/system/todefer.service`:
```
[Unit]
Description=Todefer
After=network.target postgresql.service

[Service]
WorkingDirectory=/var/todefer
SuccessExitStatus=143
ExecStart=/usr/bin/java -jar /var/todefer/todefer.jar run-app
User=deploy

[Install]
WantedBy=multi-user.target
```

Add the environment config to /var/todefer/env.edn
```
{:db-name "todefer"
 :user "todefer"
 :password "EDITME"}
```

### Run the migrations and create a Todefer login
```
sudo -u deploy java -jar /var/todefer/todefer.jar migrate
sudo -u deploy java -jar /var/todefer/todefer.jar add-user
```

### Nginx
Include this snippet in your Nginx vhost:
```
location / {
    proxy_pass http://127.0.0.1:3001/;
    proxy_set_header Host $http_host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_redirect  off;
}
```

### Start everything up

Enable and start your todefer systemd service.

Reload Nginx.

### Log into the UI

Now it should be accessible on the web. Log in and go to the settings page to
create your first pages.

## License

Copyright © Joseph Graham
