About
=====

General java object migration utility which makes it possible to persist/restore java objects regardless of possible
persistent objects code modifications. Utility extensively uses custom class loaders and bytecode transformation
internally.

Examples
========

Suppose you have persistent object

```java
import org.nohope.bytecode.migration.Migratable;
import java.io.Serializable;

@Migratable(uniqueId = "5d1ac514-e7f9-4928-8d90-01d42bb492e8",    // unique id of entity (should never change)
            dependencies = {Dependency1.class, Dependency1.class} // list of classes which also may be migrated
            )
public class Example implements Serializable {
    private static final long serialVersionUID = 1L;

    // some cool stuff here
}
```

which was modified to something like this:

```java
import org.nohope.bytecode.migration.Migratable;
import java.io.Serializable;

@Migratable(uniqueId = "5d1ac514-e7f9-4928-8d90-01d42bb492e8",
            dependencies = {AnotherDependency1.class, AnotherDependency2.class})
public class RenamedExample implements Serializable {
    private static final long serialVersionUID = 2L;

    // some other cool stuff here
}
```

All you need is to write migration script (java/groovy is supported)

```java
import org.nohope.bytecode.migration.TypedMigration;
import $.com.test.Example;        // old version (1L) of Example class prefixed with $ to avoid class names clash
import com.test.RenamedExample;   // current version (2L) of Example class

public class V1ToV2Migration implements AbstractTypedMigration<$.com.test.Example> {

  protected V1ToV2Migration() {
    super(
        "5d1ac514-e7f9-4928-8d90-01d42bb492e8", // entity uid
        1L, // source serialVersionUID
        2L  // target serialVersionUID
    );
  }

  @Override
  public RenamedExample migrate($.com.test.Example source) {
    $.com.test.Dependency1 d1 = source.getDepencency1();
    com.test.AnotherDependency1 ad1 = new com.test.AnotherDependency1(d1.getValue());
    // other migration routine ...
    return new RenamedExample(ad1);
  }
};
```

and update bytecode repository.

Now you can easily migrate old object to a new one:

```java

Repository repository = ... // migration/bytecode repository
SerializationProvider delegate = ... // original serializator used to

InputStream is = ... // stream with old version of Example object
Migrator migrator = new Migrator(delegate, repository);
RenamedExample obj = migrator.readObject(is, RenamedExample.class);
```

