# Air-Traffic-Control-System
Java simulation of an Airport Air Traffic Control system, featuring generic runway scheduling with priority queues, custom exception handling, and OOP design patterns including inheritance, polymorphism, and generics.

## Overview

This project models a real-world Air Traffic Control system capable of managing airplane scheduling, runway allocation, and maneuver permissions for an airport. An operator issues time-stamped commands that are parsed sequentially from an input file, and the system responds by updating runway states, logging flight information, and handling erroneous commands gracefully via custom exceptions.

The system handles both **takeoff** and **landing** operations, supports two airplane body types (**Wide Body** and **Narrow Body**), and enforces runway-specific rules such as cooldown periods and priority ordering.

---

## Architecture & Design

### Class Structure

```
├── Airplane                    # Base class: flight attributes, status, toString
│   ├── WideBodyAirplane        # Subclass: wide body planes
│   └── NarrowBodyAirplane      # Subclass: narrow body planes
├── Runway<T extends Airplane>  # Generic runway class with priority queue
├── IncorrectRunwayException    # Custom checked exception
├── UnavailableRunwayException  # Custom checked exception
└── Main                        # Command parser and program entry point
```

### Key Design Decisions

| Component | Choice | Reason |
|-----------|--------|--------|
| Runway collection | `PriorityQueue<T>` | O(log n) insertion and O(log n) removal with natural priority ordering |
| Runway lookup | `LinkedHashMap<String, Runway<?>>` | O(1) access by runway ID, preserves insertion order |
| Flight lookup | `HashMap<String, Airplane>` | O(1) access by flight ID for `flight_info` queries |
| Time handling | `LocalTime` | Built-in Java time API, handles HH:mm:ss parsing and arithmetic |
| Priority (landing) | `Comparator` (urgency first, then desired time) | Passed at construction to keep a single generic `Runway` class |
| Priority (takeoff) | `Comparator` (desired time ascending) | Passed at construction, no urgency concept for departures |

---

## Core Features

### Commands Supported

| Command | Description |
|---------|-------------|
| `add_runway_in_use` | Registers a new runway (landing/takeoff, wide/narrow body) |
| `allocate_plane` | Instantiates a plane and adds it to the specified runway queue |
| `permission_for_maneuver` | Clears the first priority plane and locks runway (5 or 10 min) |
| `runway_info` | Outputs current runway state to a timestamped `.out` file |
| `flight_info` | Appends flight details at query time to `flight_info.out` |
| `exit` | Terminates the program |

### Exception Handling

- **`IncorrectRunwayException`** — thrown when a plane is assigned to a runway incompatible with its operation type (e.g., a departing plane on a landing runway). Logged to `board_exceptions.out`.
- **`UnavailableRunwayException`** — thrown when `permission_for_maneuver` is issued during a runway's cooldown period. Logged to `board_exceptions.out`.

Both exceptions are thrown by `Runway` and caught in `Main`, following clean separation of concerns.

---

## OOP Concepts within project

- **Encapsulation** — all class fields are `private`, exposed only through getters/setters where necessary
- **Inheritance** — `WideBodyAirplane` and `NarrowBodyAirplane` extend `Airplane`, overriding `toString()` to prepend the body type
- **Polymorphism** — `Runway<T extends Airplane>` operates uniformly over both airplane subtypes; collections store base type references
- **Abstraction** — `Airplane` defines shared behavior; subclasses specialize only what differs
- **Generics** — `Runway<T extends Airplane>` enforces compile-time type safety, preventing a Wide Body plane from being scheduled on a Narrow Body runway

---

## Project Structure

```
src/
└── main/
    ├── java/
    │   └── org/example/
    │       ├── Airplane.java
    │       ├── WideBodyAirplane.java
    │       ├── NarrowBodyAirplane.java
    │       ├── Runway.java
    │       ├── IncorrectRunwayException.java
    │       ├── UnavailableRunwayException.java
    │       └── Main.java
    └── resources/
        ├── 00-test/
        │   ├── input.in
        │   ├── flight_info.out
        │   ├── board_exceptions.out
        │   └── runway_info_<ID>_<timestamp>.out
        └── ...
```

---

## Getting Started

### Build & Run

```bash
# Clone the repository
git clone https://github.com/<your-username>/air-traffic-control-java.git
cd air-traffic-control-java

# Build the project
./gradlew build

# Run with a specific test folder
./gradlew run --args="00-test"
```

The program reads from `src/main/resources/<test-name>/input.in` and writes output files to the same directory.

### Input Format

```
<HH:mm:ss> - <command> - <params...>
```

**Example:**
```
00:10:00 - add_runway_in_use - Charlie01 - landing - wide body
10:45:00 - allocate_plane - wide body - A380 - LX2208 - Bucharest - New York - 11:00:00 - Charlie01
14:00:00 - permission_for_maneuver - Charlie01
12:15:00 - runway_info - Charlie01
12:15:00 - flight_info - LX2208
23:00:00 - exit
```

---

## Output Files

| File | Contents |
|------|----------|
| `runway_info_<ID>_<HH-mm-ss>.out` | Runway status (free/occupied) and ordered queue of waiting planes |
| `flight_info.out` | Timestamped flight snapshots at query time |
| `board_exceptions.out` | Exception log with timestamp and message |

---

## Collection Choices & Rationale

**`PriorityQueue<T>` for runway queues** — Provides O(log n) insertions and O(log n) poll for the highest-priority airplane, which is exactly what the landing/takeoff scheduling requires. A custom `Comparator` is injected at runway construction time — takeoff runways sort by desired time only, while landing runways sort by urgency first, then desired time. This keeps the `Runway` class unified without needing subclasses per operation type.

**`HashMap<String, Airplane>` for flights** — `flight_info` queries must find a plane by its flight ID in O(1). A `HashMap` achieves this with minimal memory overhead.

**`LinkedHashMap<String, Runway<?>>` for runways** — Runway lookups by ID are O(1), and insertion order is preserved for deterministic output ordering, which matters when iterating over all runways.

---
