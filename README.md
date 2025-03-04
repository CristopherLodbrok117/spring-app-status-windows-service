# Spring app as a windows service 2025

We are going to use Winsw to install an API developed in Spring Boot as a windows service. For this example our API offers the CRUD operations on champions from the database. Let's assume that by requirements the delete operation is performed as a "logic delete", which means that the database keeps the record and only changes the attribute "active" to false, thus it's not allowed to remove records
from the database.
The API counts with a service that creates backups on every update, checks periodically the database state and if it detects that the database was corrupted under the statement 
mentioned before, it will fully restore the database.

In this occation let's see the most relevant methods (you can check all the project in the repository). Also remember that configuring the environment variables will help you
avoid absolute paths

Corruption criteria
```java
public boolean dbCorrupted(){
        return championRepository.count() < totalItems;
    }
```

Create backup:
```java
public void createBackup(){
        totalItems = championRepository.count();

        String mysqldump = "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysqldump";
        ProcessBuilder processBuilder = getProcessBuilder(mysqldump, OUTPUT_REDIRECT);

        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("Backup successful!");
            } else {
                System.out.println("Backup failed with exit code " + exitCode);
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.out.println("Error occurred during backup: " + e.getMessage());;
        }
    }
```

Restore database:
```java
public void restoreDB(){
        String mysql = "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql";
        ProcessBuilder processBuilder = getProcessBuilder(mysql, INPUT_REDIRECT);

        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("Database restored successfully!");
            } else {
                System.out.println("Restore failed with exit code " + exitCode);
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.out.println("Error occurred during restore: " + e.getMessage());;
        }
    }
```

Process builder:
```java
private static ProcessBuilder getProcessBuilder(String commandToExecute, int redirectionType) {

        String dbUser = "root";
        String dbPassword = "1234";
        String dbName = "status_service_db";
        String backupPath = "C:\\Users\\kim_j\\Desktop\\service_backup.sql";

        List<String> command = new ArrayList<>();

        command.add(commandToExecute);
        command.add("-u" + dbUser);
        command.add("-p" + dbPassword);
        command.add(dbName);

        ProcessBuilder processBuilder = new ProcessBuilder(command);

        if(redirectionType == OUTPUT_REDIRECT){
            processBuilder.redirectOutput(new File(backupPath));
        }
        else{
            processBuilder.redirectInput(new File(backupPath));
        }

        return processBuilder;
    }
```

Scheduled database state check
```java
@Scheduled(fixedDelay =  5000)
    public void monitoringDatabase(){

        if(dbCorrupted()){
            restoreDB();
            System.out.println(LocalDateTime.now() + " - database restored succesfully!");
        }
    }
```

We also need to configure the pom.xml by adding the tags `finalName` and `executable` the next way:

![pom tags](https://github.com/CristopherLodbrok117/spring-app-status-windows-service/blob/2229d6d41ae0727bee097631f7c0c59bb41fa9cb/screenshots/0%20-%20define%20app%20name.png)

<br>

## Download 
Once you have your application we can continue to prepare all the files we require. Firstly we download the last release of Winsw (in this example we use v2.12.0), check all the 
[releases](https://github.com/winsw/winsw/releases "Winsw releases"). Only the next two files are required to download:

![download winsw](https://github.com/CristopherLodbrok117/spring-app-status-windows-service/blob/cd60ec1a89f0ca2f347bfe56d26e8b511dbc6e7f/screenshots/3%20-%20download%20winsw.png)

For more configuration options or information you can check the Winsw repository [here](https://github.com/winsw/winsw "Winsw repository")

Now lets obtain the jar file. Firstly we execute the Maven tasks `clean` and `install`:

![Maven tasks](https://github.com/CristopherLodbrok117/spring-app-status-windows-service/blob/cd60ec1a89f0ca2f347bfe56d26e8b511dbc6e7f/screenshots/1%20-%20Maven%20tasks.png)

Then we go to the target folder and copy the jar file:

![jar file](https://github.com/CristopherLodbrok117/spring-app-status-windows-service/blob/cd60ec1a89f0ca2f347bfe56d26e8b511dbc6e7f/screenshots/2%20-%20Copy%20jar%20file.png)

<br>

## Configuration

Now we have all the files required to continue. The next step is to create a folder in your file explorer, in this example our route is the next one "C:\WindowsServiceSpringboot".
Here we paste our jar file and the downloaded files. As you can see there are more files, but they are generated once we configure and install the service. Initially we only have the 
next ones:

* `windows_service_spring.jar`
* `WinSW.NET4.exe`
* `WinSW.NET4.xml` (originally it was sample-minimal.xml, but we need to rename it the same as the exe)

![files required](https://github.com/CristopherLodbrok117/spring-app-status-windows-service/blob/cd60ec1a89f0ca2f347bfe56d26e8b511dbc6e7f/screenshots/4%20-%20files.png)

Open the WinSW.NET4.xml and configure the next tags this way using your id and your application data:

![winsw config](https://github.com/CristopherLodbrok117/spring-app-status-windows-service/blob/cd60ec1a89f0ca2f347bfe56d26e8b511dbc6e7f/screenshots/5%20-%20winsw%20xml%20configuration.png)

<br>

## Installing the service

Open cmd with admin permissions. Get to the folder we created and write the next command: `WinSW.NET4.exe install` (make sure you saved your xml file changes previously)

Great! it was installed succesfully, we are almost done

![install service](https://github.com/CristopherLodbrok117/spring-app-status-windows-service/blob/cd60ec1a89f0ca2f347bfe56d26e8b511dbc6e7f/screenshots/6%20-%20Installed%20service.png)

<br>

## Start the service

Open Windows Services and localize your service, select it and click the start button:

![windows service](https://github.com/CristopherLodbrok117/spring-app-status-windows-service/blob/cd60ec1a89f0ca2f347bfe56d26e8b511dbc6e7f/screenshots/7%20-%20Service%20start.png)

Now it's running, well done

![service running](https://github.com/CristopherLodbrok117/spring-app-status-windows-service/blob/cd60ec1a89f0ca2f347bfe56d26e8b511dbc6e7f/screenshots/8%20-%20Service%20running.png)

<br>

## Testing the service

The funniest part has come. Let's test our applications. For this example we're using Insomnia, it allow us create and customize requests to test our APIs. Check the [Insomnia web page](https://insomnia.rest/) for more information.


1. List all the initial champions:

![list champions](https://github.com/CristopherLodbrok117/spring-app-status-windows-service/blob/cd60ec1a89f0ca2f347bfe56d26e8b511dbc6e7f/screenshots/9%20-%20list%20initial%20champions.png)


2. Save a new champion:

![cave champion](https://github.com/CristopherLodbrok117/spring-app-status-windows-service/blob/cd60ec1a89f0ca2f347bfe56d26e8b511dbc6e7f/screenshots/10%20-%20Save%20new%20Champion.png)

3. List the champions with the new one (a backup is made after every update):

![update list champions](https://github.com/CristopherLodbrok117/spring-app-status-windows-service/blob/cd60ec1a89f0ca2f347bfe56d26e8b511dbc6e7f/screenshots/11%20-%20list%20new%20champions.png)

4. Delete all the champions from the database :

![delete champions](https://github.com/CristopherLodbrok117/spring-app-status-windows-service/blob/cd60ec1a89f0ca2f347bfe56d26e8b511dbc6e7f/screenshots/12%20-%20Delete%20all%20champions.png)


5. Now we can list our champions again and they look exactly the same as the last update. But if you want to play and you are fast enough, you can send a delete request and list all the champions immediately to see if it returns an empty list, before the restoration (remember that you have at most five seconds):

![list restored champions](https://github.com/CristopherLodbrok117/spring-app-status-windows-service/blob/c459f3110bfcd87928988088b0fa570e214ce191/screenshots/13%20-%20restored%20champions%20automatically.png)

6. We shut down our service and as we can see it's not available until we start it again:

![service shutdown](https://github.com/CristopherLodbrok117/spring-app-status-windows-service/blob/c459f3110bfcd87928988088b0fa570e214ce191/screenshots/14%20-%20service%20down.png)


