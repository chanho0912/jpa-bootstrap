# jpa-bootstrap

## 1단계 Metadata

### 요구사항 1 - @Entity 엔터티 어노테이션이 있는 클래스만 가져오기
```java
ComponentScanner scanner = new ComponentScanner();
List<Class<?>> persistence = scanner.scan("persistence"); // basePackage 변경해도 괜찮습니다
```

### 요구사항 2 - scanner 로 찾은 Entity Class 정보를 통해 MetamodelImpl 에 데이터를 채워넣어보자
Binder 를 만들어서 처리해보면 더욱 좋음, 아래 코드는 참고만..
```java
public class MetamodelImpl {
private final Map<원하는 타입 ,EntityPersister> entityPersisterMap = new ConcurrentHashMap<>();
private final Map<원하는 타입 ,CollectionPersister> collectionPersisterMap = new ConcurrentHashMap<>();
...
}
```
### 요구사항 3 - Metamodel 을 활용해 리팩터링을 진행해보자
아래의 두 클래스를 추가적으로 만들어 적용해보자
```java
@Entity
public class Department {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

    private String name;

    @OneToMany
    @JoinColumn(name = "department_id")
    private List<Employee> employees;
    
    // getter, setter, constructors, 등등
}

@Entity
public class Employee {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

    private String name;
  
    // getter, setter, constructors, 등등
}
```

- [x] Component Scanner를 통해 @Entity 어노테이션이 있는 클래스들만 가져와서 Metamodel에 저장한다.
  - [x] base scan package를 properties 파일로 관리할 수 있도록 수정...
- [x] TableDefinition, EntityPersister를 사전에 Metamodel에 만들어 두고, EntityManager에서는 Metamodel을 의존하여 사용한다

## 2단계 - SessionFactory

### 요구 사항 1 - EntityManagerFactory 를 만들어 보기
```java
public class EntityManagerFactoryImpl {
    private final CurrentSessionContext currentSessionContext;
    // 메서드 및 책임을 자유롭게 추가해도 됩니다.

    public EntityManagerFactoryImpl(적절히) {
        // 구현해보기
    }

    public EntityManager openSession(적절히) {
        // 구현해보기
    }
}
```
### 요구 사항 2 - EntityManagerFactory 를 적용하여 리팩터링을 해보자

## 3단계 - Event

### 요구 사항 1 - EventType 을 활용해 리팩터링 해보기
### 요구 사항 2 - Queue 를 활용해 데이터베이스 반영 시 최적화 해보기
