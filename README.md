# RIFE2 bootstrap project structure

This project helps you to get started with a RIFE2 web application and Gradle.

You'll find all the pieces that are explained in the first sections of
[the documentation](https://github.com/gbevin/rife2/wiki) neatly contained
in this one project.

It's ready to run, package and deploy ... and for you to have fun developing
in a very iterative, intuitive and rewarding way.

For all things RIFE2, head on to the project website:
[https://rife2.com](https://rife2.com)

## Run the tests

```bash
./gradlew clean test
```

## Running the server

```bash
./gradlew clean run
```

Go to:

[http://localhost:8080/](http://localhost:8080/)


## Deploying the app

```bash
./gradlew clean war
```

The resulting archive will be in:
`war/build/libs`


## Making an UberJar


```bash
./gradlew clean uberJar
```

Then run it with:

```bash
java -jar app/build/libs/hello-uber-1.0.jar
```