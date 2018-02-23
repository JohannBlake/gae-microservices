# Developing and Debugging Google App Engine Microservices Locally

* [The Concept](#the-concept)
* [Routing Requests](#routing-requests)
* [Running a Build](#running-a-build)
* [Debugging Services](#debugging-services)
* [Deploying to GAE](#deploying-to-gae)


This document describes a solution to setup and debug a Google App Engine project in IntelliJ that is designed to support microservices.

If you have read the Google Cloud Platform (GCP) docs on setting up, debugging and deploying a Java app, you will discover that there is no information provided on best practices on how to deal with microservices - at least at the time of this writing.

Ideally, developers would love to create all of their services within a single IntelliJ project, set some breakpoints, hit Debug and your done. In reality, it is much more complex that this and there are a number of solutions posted on how to debug microservices.

Using the setup as described below, you will be able to run your microservices locally and only have to execute a single gradle task each time you modify your code without having to stop debugging. And as a bonus, you can debug your microservices using the exact same urls as you would use on GAE. There is no need to use localhost.

The Google Cloud SDK has a gradle plugin available that does allow you load multiple services into the local web server that ships as part of the Google Cloud SDK. The biggest issue with this plugin is that it will create a random port for each service. This means that every time you restart debugging, the urls that point to each service have a different port. This is a serious issue on several levels. If one service needs to communicate with another service, it must include the port number as part of the HTTP request. But if this port number changes every time, you would need a way to update your HTTP request each time. Either you hardcode the port in your code and then change it each time, or you use some config file that your service reads in that contains the updated port number. Either way, you need to manually update either your code or a config file.

The gradle plugin for does have a parameter that you can set for the port to use. But after playing around with this I discovered that it only sets the port number on the default service. All the other services still end up getting random port numbers. I cannot understand why any developer at Google would ever do this. This just makes no sense.

Here is some important links and info about the gradle plugin:

> [https://cloud.google.com/appengine/docs/standard/java/tools/gradle](https://cloud.google.com/appengine/docs/standard/java/tools/gradle)
>
> However, this documentation is severely lacking. It fails to describe vital steps needed to use the plugin for microservices. There are two other docs you need to read in order to fully understand how to implement this plugin for microservices:
>
> [https://cloud.google.com/appengine/docs/standard/java/tools/migrate-gradle](https://cloud.google.com/appengine/docs/standard/java/tools/migrate-gradle)
>
> The title of this document is **Migrating to the Cloud SDK-based Gradle plugin**
>
> Initially I skipped over this document when I read the word "Migrating" because I was not migrating anything. I was building a project from scratch. But if you read the contents of that document, the section titled **Migrating EAR based multi-service configurations** actually describes how multiple services are implemented when you want to debug them locally. But even what is written there is very scant on the details you need. For more details, check this out:
> 
> [https://github.com/GoogleCloudPlatform/app-gradle-plugin/blob/master/USER_GUIDE.md](https://github.com/GoogleCloudPlatform/app-gradle-plugin/blob/master/USER_GUIDE.md)
>
> Scroll down to the section labeled **How do I run multiple modules on the Dev App Server v1?**
>
> Finally, there is a sample app that you can download that shows multiple modules and how the plugin can be used to test them locally. It is available at:
>
> [https://github.com/patflynn/appengine-potpourri/tree/cloud-sdk-refactor](https://github.com/patflynn/appengine-potpourri/tree/cloud-sdk-refactor)

The following documentation makes some changes to how the gradle plugin is used in order to streamline the debugging process.

When creating a build.gradle file for each service, follow the instructions at:

[https://cloud.google.com/appengine/docs/standard/java/tools/gradle](https://cloud.google.com/appengine/docs/standard/java/tools/gradle)


## <a name="the-concept"></a>The Concept

A GAE project consists of multiple modules where each module is considered a service (or microservice if you will) and each module is located as a folder at the root of the project. Each project contains its own build.gradle file. The root project also contains a build.gradle.

> NOTE: The build.gradle file in the root project must use
> ```apply plugin: "com.google.cloud.tools.appengine-standard"```
> and not:
> ```apply plugin: 'com.google.cloud.tools.appengine'```
> There's a bug in the plug and **gradle build** will fail if you use the other plugin. Your modules however should use **com.google.cloud.tools.appengine** because that is how the docs have specified it. If you're using GAE Flexible environment, use **com.google.cloud.tools.appengine-flexible** instead.

When

```gradle build```

is run from the root folder, it builds each module, which creates an output folder called **build/exploded-<module-name>** in the module's own folder, with <module-name> being the name of your module. A custom gradle task called syncBuild is then run, which runs the script syncbuild.sh which copies all the files from each of the module's build/exploded-<module-name> folder to a build folder in the project's root, which is called **build/exploded-<project-name>** where <project-name> is the name of your project.

There is no need to stop debugging because a custom gradle task called **reloadApp** is executed after the copying has completed, which causes the local web server to reload the changes. Of course, you need to first start the local web server manually which is done by running:

```gradle appengineRun```

from the project's root folder.

While the local web server (a.k.a Dev App Server) is capable of hosting multiple services, this feature is not used by the solution proposed here due to the limitation of it generating random port numbers for each service. More specifically, the appengine.run.services paramters in the build.gradle file is not used. The appengine.run.jvmFlags is used in order to specify the JVM parameters needed to launch the local web server.

You can of course use the local web server and start it manually if you want to. The advantage of starting it manually is that any exceptions generated by your app that get returned to the local web server will be displayed in the terminal. When the gradle plugin starts the local web server, it isn't shown. Only the Java icon is shown in the OS taskbar, so you won't see any detailed information about exceptions. If you want to use the local web server manually, you can find information about it here:

[https://cloud.google.com/appengine/docs/standard/java/tools/using-local-server](https://cloud.google.com/appengine/docs/standard/java/tools/using-local-server)


Running the local web server is done using this command:

```appengine-java-sdk\bin\dev_appserver.cmd [options] [WAR_DIRECTORY_LOCATION]```

Notice that the last parameter is the location where the WAR directory is. This directory is the same thing as the exploded directory that is at the project's root. Of course you can always specify any exploded directory. If you specified the exploded directory contained in a module's build directory, you would only be able to debug that module. By specifying the project's exploded directory, you can debug all the modules.

In order to combine the outputs of each module's build, you can either do it manually, which is a pain, because you would have to repeat this every time you modify your code, or you can use a gradle task to do it for you automatically. In the project's root directory, if you open up the build.gradle file, there is a task called syncBuild. You can always run the task as follows:

```gradle syncBuild```

This will copy all the files from each module's own exploded folder to build\exploded-<project-name>. You will probably seldom have to run this task on its own. Running **gradle build** automatically runs the syncBuild task. You do however need to modify the paths in the syncbuild.sh to point to each of your module's exploded folder. The exclude parameter is needed to prevent the appengine-web.xml and web.xml files from being copied. These files are needed in the combined directory but you must manually combine these and place the combined files in:

```build\exploded-<project-name>\WEB-INF```

Only one appengine-web.xml and web.xml file can appear in this folder. When combining the contents of the appengine-web.xml, you should remove any lines that specify modules. For example remove this:

```<module>company-service</module>```

Even if you don't remove it, there are no issues. But if you use the local web server's admin console:

```http://localhost:8080/_ah/```

and then click on the **Modules** link, you will see whatever the last module is listed in the appengine-web.xml file. Even if multiple modules are listed in the file, only the last one is shown. If you have no modules listed, the admin console will show only "default" as the module loaded. Just because multiple modules are not listed in the console does not mean that they are not loaded or available. They are if you combine the build outputs. To be honest, I have no idea what the purpose of the Modules page in the console is really used for. It does not appear to have any affect on your app connecting to the required module when requested.

NOTE: If you get an error when you click on the Modules link, you probably need to add a version tag to the appengine-web.xml file. But even with this error, your app is still not affected.

The syncbuild.sh scripte runs the rsync command which copies any new or changed files to the destination folder. It never deletes files. So when you place your combined appengine-web.xml and web.xml into the output folder, you don't have to worry about it being deleted each time you run the syncBuild task or syncbuild.sh script. The only time you need to update these files is when you add new modules or remove them.

It should be pointed out that originally the gradle Copy task was used to copy files. While this initially worked, over time the copying starting taking enough time that it would cause the local dev server to crash. This was because the hot loading feature supported by the JVM option:

> "-Dappengine.fullscan.seconds=5"

would detect changes and apply those changes before all the files were copied over and as a result the app would not have the correct dependencies. The rsync command runs very fast and has not yet been an issue. For a large project though where a lot of changes occur, it is still possible that the local dev server will crash. If this happens, you just need to restart it.

It is also possible that when syncbuild.sh is run that it will fail due to missing permissions on the file. For the script to be executed on a Mac, you should run the following command from a terminal window in the project's root folder:

> chmod 755 syncbuild.sh

This will allow the script to be run by other processes.

## <a name="routing-requests"></a>Routing Requests

One of our goals is also to use a consistent url that closely matches the service endpoints when the app is deployed to GAE. Using urls like 

```http://localhost:8080/company/```

isn't cool because they completely bypass the information associated with domain names and their corresponding subdomains. When microservices are deployed on GAE, they are typically in the form of:

```http://<service-id>.<project-id>.appspot.com```

For example, if the project id is **myapp-12345**:

```
http://company.myapp-12345.appspot.com/
http://employees.myapp-12345.appspot.com/
```

Often your code in the servlet that handles these urls may need to have access to the project name or subdomain. There are APIs available that will resolve those when the app runs locally but this is really unnecessary. Using the same urls when testing locally and when deploying to GAE is really the way to go. Although the urls can be identical, the port number of the local web server does need to be attached to the url. There is also the issue of how you deal with SSL. The Dev App Server cannot handle https. But in all likelihood, your deployed app to GAE will use it (and you should be using it if you are not). If you use the Charles web proxy tool, you can even map one url to a different one and even map from http to https. You can even eliminate the use of a port number. The Charles feature is known as **Map Remote** and is discussed later on. We'll assume that you don't use Charles and therefore need to handle https and port numbers. When https is used on GAE, the urls cannot use "." to separate subdomains. Instead you need to use -dot- in place of the ".". GAE will provide valid SSL certificates that use wildcards for subdomains. So the two above mentioned urls would be as follows:

```
https://company-dot-myapp-12345.appspot.com/
https://employees-dot-myapp-12345.appspot.com/
```

This means that for local testing, we can use the same urls but use http instead and include the port number:

```
http://company-dot-myapp.appspot.com:8080/
http://employees-dot-myapp.appspot.com:8080/
```

While this alone would work, we can improve on this by including the name of the service and the version of the service's API as paths as well:

```
http://company-dot-myapp.appspot.com:8080/company/v1/
http://employees-dot-myapp.appspot.com:8080/employees/v1/
```

For reasons why you would do this, you should read up on:

[https://cloud.google.com/appengine/docs/standard/java/designing-microservice-api](https://cloud.google.com/appengine/docs/standard/java/designing-microservice-api)


[https://cloud.google.com/appengine/docs/standard/java/how-requests-are-routed](https://cloud.google.com/appengine/docs/standard/java/how-requests-are-routed)

Now that we've decided how the endpoint urls are going to look, we are left with two tasks. First we need to include code in our servlets to know when the app is running locally and use http and the port number, and when it's running on GAE, in which case we use https and not include a port number. The second task is to map the subdomains to a local ip address (localhost or 127.0.0.1) in order for the urls to be resolved when debugging locally.

For code on how to detect whether your app is running locally or on GAE, check out the code in each servlet in the sample app.

To map the domain to a local address, there are two ways to do this. The cheap way is to modify your hosts file. On a Mac this is located under:

```\etc\hosts```

You'll need your Mac's admin permission to edit this file. To add the domains, using the example urls above, add these entries:

```
127.0.0.1   company-dot-myapp-12345.appspot.com
127.0.0.1   employees-dot-myapp-12345.appspot.com
```

Normally these changes take effect immediately but in some rare cases, if the DNS entry doesn't update, you can try clearing the DNA cache with:

```dscacheutil -flushcache```

The only downside to setting the domains in the hosts file is that when you want to test your app when it is running in GAE, you need to comment out the domain mappings in the host file. A second solution to avoid commenting and uncommenting out these domains is to use a tool like Charles, which is a web proxy. It has a feature known as **Map Remote**, which lets you map one domain to a different one. So you could map a call to a GAE endpoint to a local one instead without having to modify the hosts file. For more info on this feature, visit:

[https://www.charlesproxy.com/documentation/tools/map-remote/](https://www.charlesproxy.com/documentation/tools/map-remote/)

Here is what the configuration in Map Remote would look like for the sample services used in this app:

![Charles Map Remote](/images/charles-map-remote.png)

## <a name="the-concept">Running a Build
To create the build files, you use your terminal (on a Mac) and navigate to the project's root folder and execute:

```gradle build```

If you haven't already done so, manually create the appengine-web.xml and web.xml files and copy those to the combined exploded folder as was previously mentioned.


## <a name="debugging-services">Debugging Services

While IntelliJ is used here to debug the services, you can use any IDE that has the ability to attach to a JVM instance. Before you can debug the service, you need to make sure that local web server is running. You can run:

```gradle appengineRun```

Alternatively, you can run the script:

```sh runApp.sh```

which is located in the project's root folder. This will let you see any problems when it attempts to load the project and let you see any exception messages that your app might generate. Make sure to replace the path to the exploded folder in this script to point to your own project's exploded folder.

This same runApp.sh file is also present in the folder of each service. The path to the explode files is set for each of these modules. You can always run this script if you ever need to debug just the module's own code and where it does not depend on calling any external services. Just make sure that you change the path to the explode file to be where they are located within the module's own explode directory.

To debug your services, you need to setup a debug configuration in IntelliJ. Open up the **Edit Configuration** dialog (Run > Edit Configurations...)

Click on the + symbol in the top left corner to add a configuration. Scroll down and select Remote. Give your configuration a name (like **Attach to dev server**). Set the host to **localhost** and the port to 8000

Save the configuration. Then hit the Debug button. You can now use breakpoints and debug your app. Here is how the configuration settings look in IntelliJ:

![Configuration Settings](/images/intellij-config-debug.png)

So now that the local web server is running and IntelliJ is running in Debug mode, whenever you make changes to your code, all you need to do is run:

```gradle build```

This will recompile your code, copy the build output files from each module to the project's root build and reload the new build into the local web server.

There is however one minor issue to keep in mind. Even after **gradle build** has completed, you will probably have to wait about 8 seconds before the local web server has acknowledged the changes and completed updating the JVM. On my machine it took between 5 to 8 seconds. If you don't wait for this amount of time and make a request to your services, you will get served with the older version and breakpoints will not line up. If you find this annoyingly too long, you can always manually restart the local web server and then restart debugging in IntelliJ. If your fast enough and can do it under 8 seconds, that may be your preferred method. Be aware though that when you terminate the local web server (if it was started with **gradle appengineRun**), you need to wait until the JVM has stopped. The Java app icon on the OS taskbar needs to be gone. If it's not gone and you attempt to restart the web server, you will get an error indicating that the address is already in use. This doesn't happen if you start the web server manually (using **dev_appserver.sh**).

## <a name="deploying-to-gae">Deploying to GAE
To deploy your services to GAE using gradle, you can either deploy each module separately or deploy all of them together. To deploy, run:

```gradle appengineDeploy```

If you run this from the project's root folder, all the modules will be deployed. If you run this from a module's directory, only that module will be deployed. Even if you deploy from the project's root folder, the gradle task will make sure to upload each appengine-web.xml and web.xml file for each module. It will not use the files from the combined build directory.

There is however one issue that you need to be aware of when running appengineDeploy from the project's root folder. In the folder:

> build/exploded-gae-microservices/WEB-INF

are the two files:

```
appengine-web.xml
web.xml
```

As was already mentioned earlier, these are only used for debugging purposes. But if you run ```gradle appengineDeploy``` these two files will end up getting uploaded to GAE along with all the other files in this build folder. If appengine-web.xml has no <module> tag in it, the uploaded code will be treated as the default service and the code associated with the default service is all the code is the code located in this build folder. That is **not** what you want for your default service. Your default service would in fact end up containing all of your services in one service.

For this reason, you should not run ```gradle appengineDeploy``` from the project's root. Instead, you should create a script that executes ```gradle appengineDeploy``` from each service's own folder. This will cause each service to be uploaded independently. There are apparently ways of deploying multiple services simultaneously but I haven't done that yet so I cannot comment on how that is accomplished.

Even if you deploy each service independently, you still have the issue that you need to upload one of your services as the default service. To do that, you have to pick one of them to be your default and then modify the ```appengine-web.xml``` file and remove all <module> elements and then deploy that module. GAE will see that no <module> element exists and use that as your default service.

Probably the easiest way to do this is just comment out the <module> element and then deploy. But you must make sure to uncomment it and then deploy it again so that the service is loaded under its own name as well. A better solution would automate this task but I'll leave that up to you to figure out.

One problem I have encountered was when it failed to deploy and I discovered that I had to shut down Charles and comment out any of the mappings I made in the hosts file. It seems that the gradle task may need to address those urls as they would be available on GAE and not locally.