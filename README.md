# Developing and Debugging Google App Engine Microservices Locally

This document describes a solution to setup and debug a Google App Engine project in IntelliJ that is designed to support microservices.

If you have read the Google Cloud Platform (GCP) docs on setting up, debugging and deploying a Java app, you will discover that there is no information provided on best practices on how to deal with microservices - at least at the time of this writing.

Ideally, developers would love to create all of their services within a single IntelliJ project, set some breakpoints, hit Debug and your done. In reality, it is much more complex that this and there are a number of solutions posted on how to debug microservices.

There is a gradle plugin available that does allow you load multiple services into the local dev server that ships as part of the Google Cloud SDK. The biggest issue with this plugin is that it will create a random port for each service. This means that every time you restart debugging, the urls that point to each service have a different port. This is a serious issue on several levels. If one service needs to communicate with another service, it must include the port number as part of the HTTP request. But if this port number changes every time, you would need a way to update your HTTP request each time. Either you hardcode the port in your code and then change it each time, or you use some config file that your service reads in that contains the updated port number. Either way, you need to manually update either your code or a config file.

The gradle plugin for does have a parameter that you can set for the port to use. But after playing around with this I discovered that it only sets the port number on the default service. All the other services still end up getting random port numbers. I cannot understand why any developer at Google would ever do this. This just makes no sense.

If you really want to use the gradle plugin for running your app locally, you'll find information about it here:

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

Even though I don't use the plugin for running the app locally, I do use it for deploying the app to the GAE. So be sure to install the plugin as described at:

[https://cloud.google.com/appengine/docs/standard/java/tools/gradle](https://cloud.google.com/appengine/docs/standard/java/tools/gradle)

Building your app with Gradle is not required however to setup a project or debug your app locally. I do use a custom gradle task that is part of the debugging phase but you can replace this with some other method that is more in line with whatever packaging/deployment tools you use. More info on this is described later on below.


## The Concept

A GAE project consists of multiple modules where each module is considered a service (or microservice if you will) and each module is located as a folder at the root of the project. Each project contains its own build.gradle file and when

```gradle assemble```

is run from within a module's folder, it creates an output folder called **build** which in turn contains an **exploded** folder containing all the files that eventually are used when the module is deployed locally or to the GAE.

The Google Cloud SDK includes a local web server (a.k.a Dev App Server) to host your app locally. The current version of this web server is capable of hosting multiple modules simultaneously. The gradle plugin for GAE handles how multiple modules are loaded (as previously described). But as was mentioned, every module will generate a service running under a random port number, which is what we want to avoid.

The documentation on using the Dev App Server is found here:

[https://cloud.google.com/appengine/docs/standard/java/tools/using-local-server](https://cloud.google.com/appengine/docs/standard/java/tools/using-local-server)

Running the Dev App Server is done using this command:

```appengine-java-sdk\bin\dev_appserver.cmd [options] [WAR_DIRECTORY_LOCATION]```

Notice that the last parameter is the location where the WAR directory is. This directory is the same thing as the exploded directory mentioned above. What this means is that the Dev App Server can only reference a single exploded directory. And here is the problem. If you have multiple modules making up multiple services, how is it even possible to load each one into a single running instance of the Dev App Server?

The gradle plugin is able to do this although how it does it is not documented. But as mentioned a few times already, each module that it does upload to the Dev App Server gets assigned a random port number.

The key to bypassing the need for random port numbers is to combine the build outputs from each module into a single build directory and use that for the WAR_DIRECTORY_LOCATION parameter.

An alternative approach would be to run a separate instance of the Dev App Server for each module, assigning each instance its own unique port. This approach is fine if you only had a few modules but would be a nightmare if you had a lot. You would also have to open up each module as a separate project in IntelliJ and attach each project to the running instance. For this reason, it really makes no sense to use multiple instances of the App Dev Server.

In order to combine the outputs of each module's build, you can either do it manually, which is a pain, because you would have to repeat this every time you mnodify your code, or you can use a gradle task to do it for you automatically. In the project's root directory, if you open up the build.gradle file, you see a task called CopyBuild. You run the task as follows:

```gradle CopyBuild```

This will copy all the files from each module's own exploded folder to build\exploded-services. You need to modify the paths in the CopyBuild task to point to each of your module's exploded folder. The exclude parameter is needed to prevent the appengine-web.xml and web.xml files from being copied. These files are needed in the combined directory but you must manually combine these and place the combined files in:

```build\exploded-services\WEB-INF```

Only one appengine-web.xml and web.xml file can appear in this folder. When combining the contents of the appengine-web.xml, you should remove any lines that specify modules. For example remove this:

```<module>company-service</module>```

Even if you don't remove it, there are no issues. But if you use the Dev App Server's admin console:

```http://localhost:8080/_ah/```

and then click on the **Modules** link, you will see whatever the last module is listed in the appengine-web.xml file. Even if multiple modules are listed in the file, only the last one is shown. If you have no modules listed, the admin console will show only "default" as the module loaded. Just because multiple modules are not listed in the console does not mean that they are not loaded or available. They are if you combine the build outputs. To be honest, I have no idea what the purpose of the Modules page in the console is really used for. It does not appear to have any affect on your app connecting to the required module when requested.

NOTE: If you get an error when you click on the Modules link, you probably need to add a version tag to the appengine-web.xml file. But even with this error, your app is still not affected.

The CopyBuild task copies and overwrites existing files. It never deletes files. So when you place your combined appengine-web.xml and web.xml into the output folder, you don't have to worry about it being deleted each time you run CopyBuild. The only time you need to update these files is when you add new modules or remove them.

## Routing Requests

One of our goals is also to use a consistent url that closely matches the service endpoints when the app is deployed to GAE. Using urls like 

```http://localhost:8080/company/```

isn't cool because they completely bypass the information associated with domain names and their corresponding subdomains. When microservices are deployed on GAE, they are typically in the form of:

```http://<service-id>.<project-id>.appspot.com```

For example, if the project id is **myapp-12345**:

```
http://company.myapp-12345.appspot.com/
http://employees.myapp-12345.appspot.com/
```

Often your code in the servlet that handles these urls may need to have access to the project name or subdomain. There are APIs available that will resolve those when the app runs locally but this is really unnecessary. Using the same urls when testing locally and when deploying to GAE is really the way to go. Although the urls can be identical, the port number of the App Dev Server does need to be attached to the url. There is also the issue of how you deal with SSL. The Dev App Server cannot handle https. But in all likelihood, your deployed app to GAE will use it (and you should be using it if you are not). When https is used on GAE, the urls cannot use "." to separate subdomains. Instead you need to use -dot- in place of the ".". GAE will provide valid SSL certificates that use wildcards for subdomains. So the two above mentioned urls would be as follows:

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
http://employees-dot-myapp.appspot.com:8080/employees/v1
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

## Running a Build
To create the build files, you use your terminal (on a Mac) and navigate to the module's folder (not the project's root folder). Then you execute:

```gradle assemble```

Note: If you installed the Cloud SDK gradle plugin, don't run:

```gradle appengineRun```

While that builds the output files, it also runs the Dev App Server. You don't want to do that because you need to launch the Dev App Server after you've combined the output files.

After you've built each module, use your terminal and change to the project's root and run:

```gradle CopyBuild```

If you haven't already done so, manually create the appengine-web.xml and web.xml files and copy those to the combined exploded folder as was previously mentioned.


## Debugging the Services

While IntelliJ is used here to debug the services, you can use any IDE that has the ability to attach to a JVM instance. Before you can debug the service, you need to make sure that App Dev Server is running. The sample app includes a bash script that you can run. In the project's root folder is a file called runApp.sh. To run this, in a terminal run:

```sh runApp.sh```

This will launch App Dev Server and load the exploded files.

This same runApp.sh file is also present in the folder of each service. The path to the explode files is set for each of these modules. You can always run this script if you ever need to debug just the module's own code and where it does not depend on calling any external services. Just make sure that you change the path to the explode file to be where they are located within the module's own explode directory.

To debug your services, you need to setup a debug configuration in IntelliJ. Open up the **Edit Configuration** dialog (Run > Edit Configurations...)

Click on the + symbol in the top left corner to add a configuration. Scroll down and select Remote. Give your configuration a name (like **Attach to dev server**). Set the host to **localhost** and the port to 8000

Save the configuration. Then hit the Debug button. You can now use breakpoints and debug your app. Here is how the configuration settings look in IntelliJ:

![Configuration Settings](/images/intellij-config-debug.png)

## Deploying to GAE
To deploy your services to GAE using gradle, you need to deploy each module separately although a gradle task could be used to do all of them at once. To deploy a module, use your terminal and navigate to the module's folder and run:

```gradle appengineDeploy```

This task is only available in the Google Cloud SDK gradle plugin.
