# ♕ Chess Application

This project demonstrates mastery of proper software design, client/server architecture, networking using HTTP and WebSocket, database persistence, unit testing, serialization, and security.

## 10k Architecture Overview

The application implements a multiplayer chess server and a command line chess client.

[![Sequence Diagram](10k-architecture.png)](https://sequencediagram.org/index.html#initialData=C4S2BsFMAIGEAtIGckCh0AcCGAnUBjEbAO2DnBElIEZVs8RCSzYKrgAmO3AorU6AGVIOAG4jUAEyzAsAIyxIYAERnzFkdKgrFIuaKlaUa0ALQA+ISPE4AXNABWAexDFoAcywBbTcLEizS1VZBSVbbVc9HGgnADNYiN19QzZSDkCrfztHFzdPH1Q-Gwzg9TDEqJj4iuSjdmoMopF7LywAaxgvJ3FC6wCLaFLQyHCdSriEseSm6NMBurT7AFcMaWAYOSdcSRTjTka+7NaO6C6emZK1YdHI-Qma6N6ss3nU4Gpl1ZkNrZwdhfeByy9hwyBA7mIT2KAyGGhuSWi9wuc0sAI49nyMG6ElQQA)

## Modules

The application has three modules.

- **Client**: The command line program used to play a game of chess over the network.
- **Server**: The command line program that listens for network requests from the client and manages users and games.
- **Shared**: Code that is used by both the client and the server. This includes the rules of chess and tracking the state of a game.

## Starter Code

As you create your chess application you will move through specific phases of development. This starts with implementing the moves of chess and finishes with sending game moves over the network between your client and server. You will start each phase by copying course provided [starter-code](starter-code/) for that phase into the source code of the project. Do not copy a phases' starter code before you are ready to begin work on that phase.

## IntelliJ Support

Open the project directory in IntelliJ in order to develop, run, and debug your code using an IDE.

## Maven Support

You can use the following commands to build, test, package, and run your code.

| Command                    | Description                                     |
| -------------------------- | ----------------------------------------------- |
| `mvn compile`              | Builds the code                                 |
| `mvn package`              | Run the tests and build an Uber jar file        |
| `mvn package -DskipTests`  | Build an Uber jar file                          |
| `mvn install`              | Installs the packages into the local repository |
| `mvn test`                 | Run all the tests                               |
| `mvn -pl shared test`      | Run all the shared tests                        |
| `mvn -pl client exec:java` | Build and run the client `Main`                 |
| `mvn -pl server exec:java` | Build and run the server `Main`                 |

These commands are configured by the `pom.xml` (Project Object Model) files. There is a POM file in the root of the project, and one in each of the modules. The root POM defines any global dependencies and references the module POM files.

## Running the program using Java

Once you have compiled your project into an uber jar, you can execute it with the following command.

```sh
java -jar client/target/client-jar-with-dependencies.jar

♕ 240 Chess Client: chess.ChessPiece@7852e922
```
## Server Sequence Diagram
https://sequencediagram.org/index.html?presentationMode=readOnly#initialData=IYYwLg9gTgBAwgGwJYFMB2YBQAHYUxIhK4YwDKKUAbpTngUSWDABLBoAmCtu+hx7ZhWqEUdPo0EwAIsDDAAgiBAoAzqswc5wAEbBVKGBx2ZM6MFACeq3ETQBzGAAYAdAE5M9qBACu2AMQALADMABwATG4gMP7I9gAWYDoIPoYASij2SKoWckgQaJiIqKQAtAB85JQ0UABcMADaAAoA8mQAKgC6MAD0PgZQADpoAN4ARP2UaMAAtihjtWMwYwA0y7jqAO7QHAtLq8soM8BICHvLAL6YwjUwFazsXJT145NQ03PnB2MbqttQu0WyzWYyOJzOQLGVzYnG4sHuN1E9SgmWyYEoAAoMlkcpQMgBHVI5ACU12qojulVk8iUKnU9XsKDAAFUBhi3h8UKTqYplGpVJSjDpagAxJCcGCsyg8mA6SwwDmzMQ6FHAADWkoGME2SDA8QVA05MGACFVHHlKAAHmiNDzafy7gjySp6lKoDyySIVI7KjdnjAFKaUMBze11egAKKWlTYAgFT23Ur3YrmeqBJzBYbjObqYCMhbLCNQbx1A1TJXGoMh+XyNXoKFmTiYO189Q+qpelD1NA+BAIBMU+4tumqWogVXot3sgY87nae1t+7GWoKDgcTXS7QD71D+et0fj4PohQ+PUY4Cn+Kz5t7keC5er9cnvUexE7+4wp6l7FovFqXtYJ+cLtn6pavIaSpLPU+wgheertBAdZoFByyXAmlDtimGD1OEThOFmEwQZ8MDQcCyxwfECFISh+xXOgHCmF4vgBNA7CMjEIpwBG0hwAoMAADIQFkhRYcwTrUP6zRtF0vQGOo+RoFmipzGsvz-BwVygYKQH+iMyzKfMizfGpOx7Es0KPMB4lUEiMAIEJ4oYoJwkEkSYCkm+hjWbZVAmkgWjok5DloK5ajuUpREoKp+h-DsVwqsGGoALLZKo4qOAZMDQDAJkAtuXnWf6ABCIahTkUYxnGhRaUmlSiWmeEETmqh5oZYxFiW9R6OuKKEmFDb0flgrDvy9SyBAMCMiybIGXONL7vewqTUyG7utosrygZmAJeqq0wGgEDMAAZr4TYjYu3kuvtvb9p5IHVP6zLTJe0BIAAXigHAVSgsYKeh8LJsgqYwOmACMTX8q1ewddA9Q+M9eqvR9ux0Wdt4OrVHY2Vdbqvs6Xm+vj9Q1EgR2WE0MXqVm2DeDZHCfRTWw7EZaw5NAn1sKo8RGfFqoagAkmgIDQCi4A5ZTOxDZjoGugj8RI5932-fGNWAyUYANeDoxjM1UOLDDpbwxRCso42DF3bu80jvUh5yCgz6UYh6Dnpe17nQKS5LY+Aau1uFsfpZ-rOeKGSqABwyYDpAOExJYH6ZFZmwZeVH1kZMCaQ90fwED2EwLh+GjPH5afCz5HJ07yFp6jDGeN4fj+F4KDoDEcSJI3zfOb4WCiYKMuNNIEb8RG7QRt0PRyWlBTDBRKfVZn2mB6WxtQO9eQFPUAA8M8V+UkeL73RN2UJXdBfYXdle5UtUuj9IyMAE1TQ7LvwRXc28nensMitDsypAMAk2TGAVpfxC2VHzGAKV5IOCyrAcUvlkDrm3khG8VsMaXS7NdPsUtCqliesvd6itow-Sqv9TCOcNYgycFrbMkN8z62LLDBUcsTYDTRqgi6MdsYYJ-utacxcorGkvJAJCHl8bDRvqOIwKBuDHl9vIZ+V5tBvwXB7Soy4ZDSKZIYHh8hsGVCjvUTup5Q4AT3rCDC0tM4vAzrHMh6scKNVGNXRidcAgonXP4bA4oNT8TRDAAA4kqDQPccH1AaP44eY97BKmnuXZBNV9GL3qPgt6a80CbyQegXeUcD6dnqMgHITk0QX1EZ2cR7DJHjWWmAJ+mS0DKIWp-apPsXzrTlDAWe21wGQLStA7KcC-KILiegFB780GcNsj2LB-tOGPWYSvZGSsSGqzquQzWENcx0Pagww28yCGm0Gu7e6eSWmKN0Rba+FTDFokCTmBRs8GkfzUUtKak0glMPprALINA0AwGESMmZDxzGlgKWAW5agTEIDAFmZY0ScwFgaOMOFKB+bSALKDcIwRAggk2PEXUKA3SckTssZIoA1SEsgqXMYyKABySoeadAssC45NQXiwqCQipFSpUXosxdi5YuL8UUpLmMb4pKQDkoTlS2l9LRUXEZaQzG9VGj506DC6lHKwlcrmDy+oGKsU4rxZOKVoqQTislfw4lGq5h0rmAy5xtdmL+A4AAdjcE4FATgYgRmCHALiAA2eAE5DDgpgEUchuTWWNFaB0KJMS5azyzDKuYNjEyJOBckl6Cy0kZOGWgcoSalS2pQEyr8kbbK23ROCjEyKSmFptUqVNg5LljNvlUx+l57mv1GSoxaX9mA6OABtDpFcumJQgaldKMCYADIQYIl+yCjmWJOVM26Yjl1RrwVm-ZSy-orOzvYyh1Cda0LagbOGezkasIYku9B9RB1Xyxj5QZdsMRwGDeCutoxk0lrHbtQW8D-JvIbXMGAGJV09FJWgNUpSuEsuKqVFAfVypEOVnPWxSq1mUILjQzZZ6dldRDDAXqblr2PvdmNe+76jwoGrT+x5GNnn1HFAMZg4K8ZlMxgYmAlbaNKkhdC793LpClqsqEmAelrUorRXqvl6dFVq2Bg0VVEn2U6pkzAfVgR5ODUdfXSw0j7KbBbkgBIYADN9ggMZgAUhAcUASKz+HNeG9WkbJJNGZDJHoyLYkLvQNTBAwADNQDgBAeyUA1jItRamix6avyZsRtmqqua-P5oIlFmTWmVg-EC8F0L4W9gAHUWD8xHg2HJd6YAACs7NoGrbZ8Un6kNuWJFmOpiwt55vKAcDLtQss5Vy5QfLsMxjFdKxGBsFy75XOqbUvNrXRjtbGJ11L3WrhLqYzAMUEpB3DrqX+5Kk6+mwLQIBoZqWe2NMq6uvRWM5kpMIZVPd89MOHrBhslqWzz1MIewcthraPaVYfVNij1S6PCYY4uTb231zsbafKDL9bpN9b5bzcdoaDrHVOjeiRLLJk3Vu33EqHAL67pVi9xTud0w4ZPXh6GBHZREZI-1Zxt6JlXTh+c9d7OMFnbtgNoLQ2wvQAC4LkLwu6ijZK2VtH-7TuDIF3liXhOrEwGJ6T1DyyKerLe443Dn38OdUZz1ZrLOzaPs8vUdAYcUQ8YlzAVQ9ljoohQFmZnuIOCLDGzLg78B7cmjNDWcM6G4Mbv9IGQPYYkJk5DxYynFD0yZm1rrL7DOA-BnNH84PZGQcSLhtgAKfG5hNEG1AGtwnVKl+GxFss7wlSQ9UUKV0Bf+ec6HcLcLxp5L2DQHMDAl2P6Vbb0j1FKODXaiNQSyKiw-IqAOOa4VXYxg6AgDoSb66A4ZpgA1ur-H-xQqzKJuPsywIxazsq-Oh-zd6YCF4ILJmzN3-lIgYMsBgDYGwMgEAaSw0hJP2EgeIeEeMeYwRVOLOEaxMxMtSrEAbgPADEBbEYJtHcFtFRG2OAqAJ+BvPtDRGRQwE0BAU5D0NnJ9K6MOfccjPPHjDAt0VQBAgfRjJvXArRSsQg3GLcEgy3B3HwCg3PGbWA1-cFOg7AppaQTRdEVghzOYYg3HDdWycgkcW7bjF-PAATK-CrcTJAhTHXYGS-Jxc3QaIAA
