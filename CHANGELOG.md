# changelog

This file summarizes **notable** changes for each release, but does not describe internal changes unless they are particularly exciting. This change log is ordered chronologically, so each release contains all changes described below it.

----

## <a name="Unreleased"></a>Unreleased Changes

## <a name="0.2.0"></a>New and Noteworthy for Version 0.2.0

Many thanks to Jakub Kozłowski and Taylor Brown for their contributions to this release.

Updates for new features available in upstream libraries.

- [#32](https://github.com/ChristopherDavenport/linebacker/pull/32) Uses utilities exposed via `ContextShift` to provide the shifting construct in a non-implementation dependent way(such that fairness guarantees of the context may not make asumptions valid).
- [#29](https://github.com/ChristopherDavenport/linebacker/pull/29) Added capability to use a configurable `ThreadFactory` to generate the `ExecutorService`'s.
- [#23](https://github.com/ChristopherDavenport/linebacker/pull/23) `DualContext` now extends `Linebacker` as it has the capability to operate as either construct.
- [#19](https://github.com/ChristopherDavenport/linebacker/pull/19) `ExecutorService` exposed as resources not as Streams.

Dependencies:

- cats-effect 1.0.0
- Dropped fs2 dependency

## <a name="0.1.0"></a>New and Noteworthy for Version 0.1.0

Initial Stable Release. Featuring `Linebacker` for blocking using the implicit pattern, `DualContext` which contains both execution contexts, and `Quarterback` for more advances thread shifting techniques. ExecutorServices exposed as
streams for resource management.

- cats-effect 0.10.1
- fs2-core 0.10.3
