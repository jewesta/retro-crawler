### Model discovery

```java
Model model = Model.from("com.example.collection");

RetroCrawler crawler = RetroCrawler.builder()
		.model(model)
		.repository(repository)
		.build();
```
