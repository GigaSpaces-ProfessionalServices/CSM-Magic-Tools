# gs-vault

Local encrypted credential store. Stores key/value secrets in a SQLite database using AES-256-CBC encryption (PBKDF2WithHmacSHA256 key derivation).

## Build

```bash
mvn clean package
```

Produces: `target/gs-vault-1.0-SNAPSHOT-jar-with-dependencies.jar`

## Configuration

Default config is bundled in the JAR (`app.properties`). Override with `--config`:

| Property | Default | Description |
|---|---|---|
| `app.secretKey` | `f1b4Fy2m6N2icOB5H0myUaA1FaY3fIxO` | Encryption key |
| `app.algorithm` | `AES` | Cipher algorithm |
| `app.keySize` | `256` | Key size in bits |
| `app.cipherMode` | `AES/CBC/PKCS5Padding` | Cipher mode |
| `app.db.path` | `/path/to/resources/` | Directory for vault.db |
| `app.db.name` | `vault.db` | SQLite database filename |

Override DB path/name at runtime via JVM system properties:
```bash
java -Dapp.db.path=/custom/path/ -Dapp.db.name=myvault.db -jar gs-vault-1.0-SNAPSHOT-jar-with-dependencies.jar ...
```

## Usage

```bash
JAR=gs-vault-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Initialize vault (create DB + default entries)

```bash
java -jar $JAR --init
```

Creates the `vault` table and inserts default credential `storepass=123456` (encrypted).

### Store a secret

```bash
java -jar $JAR --set <key>=<value>
```

Example:
```bash
java -jar $JAR --set manager_pass=mySecretPassword
java -jar $JAR --set db_password=s3cr3t
```

### Retrieve a secret

```bash
java -jar $JAR --get <key>
```

Example:
```bash
java -jar $JAR --get manager_pass
# prints: mySecretPassword
```

### Use custom config file

```bash
java -jar $JAR --config=/path/to/app.properties --get manager_pass
```

### Help

```bash
java -jar $JAR --help
```

## Encryption Details

- Algorithm: AES-256-CBC with PKCS5 padding
- Key derivation: PBKDF2WithHmacSHA256, 65536 iterations
- IV: random 16-byte prepended to ciphertext, Base64-encoded in DB

## Database Schema

SQLite table `vault`:

```sql
CREATE TABLE vault (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  propertyName   TEXT NOT NULL UNIQUE,
  encryptedValue TEXT NOT NULL UNIQUE
);
```

## Notes

- `--set` on an existing key will fail (UNIQUE constraint). Delete the row manually to update.
- `--get` on a missing key prints nothing (returns empty string, no error).
