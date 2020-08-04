# elastic-capture-method-data
A **Java** library that must be used with **Spring Boot** and APM agent. It will send spans to an APM server to log methods parameters and/or responses as labels.
# Usage

In your pom.xml use:

``` xml
<dependency>
   <groupId>com.gitlab.klabs</groupId>
   <artifactId>elastic-capture-method-data</artifactId>
   <version>1.0.0</version>
</dependency>
```

and

``` xml
<repositories>
   <repository>
     <id>jitpack.io</id>
     <url>https://jitpack.io</url>
   </repository>
</repositories>
```

and in your main class:

```java
@SpringBootApplication
@EnableElasticCaptureMethodData
public class Application{
  //code here
}
```

In your Spring Boot Rest Controller you can use:

```java
@PostMapping("test")
@CaptureTransaction
/*
* You can use @ElasticCaptureMethodData
* to send to APM method params and method response 
* or with (parameters = false) or (response = false)
* to not send one of them.
* */
@ElasticCaptureMethodData(parameters = false)
public ResponseEntity myMethod(@RequestParam Integer id) {
   //your code here
}
```

Remember that span is inside a transaction. Then, you must use **@CaptureTransaction** in the Controller method. It will be the parent of all spans. 

And the response of a controller method MUST BE an object of class **ResponseEntity**. With that the library will be able to send the return to the transaction as a custom context and will not create a span. For the other methods your return can be any Object.

In case of following inappropriate usage the library will NOT create a span:

- **@ElasticCaptureMethodData** in a method that has no parameters and returns void.
- **@ElasticCaptureMethodData(response = false)** in a method that has no parameters.
- **@ElasticCaptureMethodData(parameters = false)** in a method that returns void.
- **@ElasticCaptureMethodData(parameters = false, response = false)**.

# ResponseEntityExceptionHandler

In case you have a **ResponseEntityExceptionHandler** like the example below the library will put the response in the transaction
as if method controller was executed with the **ResponseEntity** of the method.

```java
@ControllerAdvice
public class HttpExceptionHandler extends ResponseEntityExceptionHandler {
	@ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnableToGetResumedDebitsException(Exception e, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }
}
```

# Issues

In case you face exceptions related to JoinPoint/CodeSignature class in your application initialization try to put the following dependency in your **pom.xml**:

``` xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

# Generating another version

1 - Create a new branch from the master branch.

2 - Make your modifications.

3 - Update the **pom.xml** version section, the **README.md** file, tag your last commit with the new version and push everything.

4 - Merge the code into the master branch. Check the permissions to do it.
