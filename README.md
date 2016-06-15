[![Javadoc](https://img.shields.io/badge/javadoc-reference-blue.svg)](https://chirp.arashpayan.com/android/docs)
[![Latest Maven Central Version](https://img.shields.io/maven-central/v/com.arashpayan/chirp.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.arashpayan%22%20AND%20a%3A%22chirp%22)
## Chirp (Android)

Chirp is a network service discovery protocol. In short, it's a simpler and more reliable alternative to mDNS/Bonjour. For more information about the protocol and libraries for different languages, see the [official Chirp homepage](https://chirp.arashpayan.com).

## Installation with Gradle
```
dependencies {
    compile 'com.arashpayan:chirp:0.2.0'
}
```

## Usage
#### Listening for services:
```
String serviceName = "com.example.service"; // use "*" to listen for all services
ChirpBrowser browser = Chirp.browseFor(serviceName).
                             listener(this).
                             start(getApplication());
```

#### Publishing a service:
```
ChirpPublisher publisher = Chirp.publish("com.example.service").
                                 start(getApplication());
```

You can even include a payload of arbitrary key values (serializable to JSON) to publish with your service:
```
Map<String, Object> payload = new HashMap<>();
payload.put("port", 1337);
payload.put("serial_number", "thx1138");
ChirpPublisher publisher = Chirp.publish("com.example.service").
                                 payload(payload).
                                 start(getApplication());
```

When you no longer want your service published:
```
publisher.stop();
```
