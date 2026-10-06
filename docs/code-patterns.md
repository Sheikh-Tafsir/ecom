# Code Patterns

Concrete, copy-paste patterns used throughout this codebase. Follow these exactly for consistency.

---

## Spring Boot (Main Server)

### Controller

```java
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final MessageService messageService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> findById(@PathVariable Long id) {
        RoleResponse role = roleService.findById(id);
        return ResponseUtils.ok(role, messageService.get("successfully.found", "Role"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(@Valid @RequestBody RoleRequest request) {
        Long id = roleService.create(request.name(), request.permissions());
        return ResponseUtils.created(id, messageService.get("successfully.created", "Role"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ResponseUtils.ok(messageService.get("successfully.deleted", "Role"));
    }
}
```

**Rules:**
- Always `@RequiredArgsConstructor` — no `@Autowired`.
- Always `@Valid` on `@RequestBody` params.
- Always return `ResponseEntity<ApiResponse<T>>` via `ResponseUtils`.
- Use `ResponseUtils.ok()` for 200, `ResponseUtils.created()` for 201.
- Messages via `MessageService.get(key, entityName)` — no hardcoded strings.

---

### Service

```java
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    @PreAuthorize("hasAnyAuthority(T(com.example.ecom.common.enums.Permission).ADMIN_ACCESS.getValue()," +
            "T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Cacheable(value = CACHE_ROLES)
    public List<RoleResponse> findAll() {
        return roleRepository.findAll().stream()
                .map(RoleResponse::new)
                .toList();
    }

    @PreAuthorize("hasAuthority(T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_ROLE, key = "#id"),
            @CacheEvict(value = CACHE_ROLES, allEntries = true)
    })
    public RoleResponse update(Long id, String name, Set<Permission> permissions) {
        Role role = findByIdHelper(id);
        role.setName(name);
        role.setPermissions(permissions);
        return new RoleResponse(roleRepository.save(role));
    }

    public Role findByIdHelper(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + id));
    }
}
```

**Rules:**
- `@Transactional` on every state-changing method.
- `@PreAuthorize` uses `Permission` enum — never raw strings.
- Cache names always from `CacheConstants` static imports — never literals.
- Dual eviction with `@Caching` when entity appears in both a single-item and list cache.
- Use `allEntries = true` for list caches, `key = "#id"` for single-item caches.
- Throw `EntityNotFoundException` for not-found — never return `null` from a find method that the caller expects to succeed.
- Internal helpers (e.g., `findByIdHelper`) return the JPA entity; public methods return DTOs.

---

### Entity

```java
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private Set<Permission> permissions;
}
```

**Rules:**
- Always extend `BaseEntity` — never declare `createdAt`/`updatedAt` manually.
- `BaseEntity` provides: `version` (`@Version`), `createdAt` (`Instant`), `updatedAt` (`Instant`).
- Never return an entity directly from a controller or service public method — always map to a DTO.

---

### `ApiResponse<T>` Shape

```java
// Success — data + message
return ResponseUtils.ok(data, "message");       // 200
return ResponseUtils.created(data, "message");  // 201
return ResponseUtils.ok("message");             // 200, no data body

// Error — field-level validation (from BindingResult)
return ResponseUtils.error(bindingResult);

// Error — global message
return ResponseUtils.error("Something went wrong", HttpStatus.BAD_REQUEST);
```

JSON output:
```json
// Success
{ "data": {...}, "message": "..." }

// Validation error
{ "errors": { "email": ["must be valid"], "global": ["..."] } }
```

`null` fields are omitted from JSON (`@JsonInclude(NON_NULL)`).

---

### Caching Pattern

```java
import static com.example.ecom.common.utils.CacheConstants.*;

// Read
@Cacheable(value = CACHE_PRODUCTS, key = "#id")
public ProductResponse findById(Long id) { ... }

// Write — evict both single-item and list cache
@Caching(evict = {
    @CacheEvict(value = CACHE_PRODUCTS, key = "#id"),
    @CacheEvict(value = CACHE_PRODUCTS_EDIT, key = "#id")
})
@Transactional
public ProductResponse update(Long id, ...) { ... }

// Delete — evict list entirely
@CacheEvict(value = CACHE_PRODUCTS, allEntries = true)
@Transactional
public void delete(Long id) { ... }
```

Available cache names (from `CacheConstants.java`):

| Constant                     | Value                   | Purpose                    |
|------------------------------|-------------------------|----------------------------|
| `CACHE_USER`                 | `"user"`                | Single user by id          |
| `CACHE_USERS`                | `"users"`               | User list                  |
| `CACHE_PROFILE`              | `"profile"`             | User profile               |
| `CACHE_ROLE`                 | `"role"`                | Single role by id          |
| `CACHE_ROLES`                | `"roles"`               | Role list                  |
| `CACHE_PRODUCTS`             | `"product"`             | Public product view        |
| `CACHE_PRODUCTS_EDIT`        | `"productEdit"`         | Admin product edit view    |
| `CACHE_CATEGORIES`           | `"categories"`          | Category list              |
| `CACHE_BANNERS`              | `"banners"`             | Banner list                |
| `CACHE_FAQS`                 | `"faqs"`                | FAQ list                   |
| `CACHE_BLOGS`                | `"blogs"`               | Blog list                  |
| `CACHE_REVOKED_ACCESS_TOKENS`| `"revokedAccessTokens"` | Token blacklist            |
| `CACHE_OTPS`                 | `"otps"`                | OTP codes                  |
| `CACHE_SSE_TICKETS`          | `"sseTickets"`          | One-time SSE auth tickets  |
| `CACHE_IDEMPOTENCY`          | `"idempotency"`         | Idempotency key store      |

---

### Unit Test

```java
@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleService roleService;

    @Test
    @DisplayName("findById should throw EntityNotFoundException when role does not exist")
    void findById_roleNotFound_throwsEntityNotFoundException() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> roleService.findById(99L));
        verify(roleRepository).findById(99L);
    }
}
```

**Rules:**
- `@ExtendWith(MockitoExtension.class)` — no `@SpringBootTest` for unit tests.
- `@DisplayName` with a readable sentence on every test.
- Method name: `methodName_condition_expectedResult` (BDD-style).
- Always `verify()` that the expected collaborator was called.
- Use `@BeforeEach setUp()` to initialize shared test state.

---

## Node.js (Chat Server)

### Controller (Router)

```js
const express = require('express');
const AsyncHandler = require('express-async-handler');
const AuthenticationMiddleware = require('../middleware/AuthenticationMiddleware');
const { ok, created } = require('../utils/ResponseUtils');
const { FOUND, CREATED } = require('../utils/Messages');
const ChatService = require('../service/ChatService');

const router = express.Router();

// GET /chats
router.get('', AuthenticationMiddleware, AsyncHandler(async (req, res) => {
    const data = await ChatService.findAllChatsByUserId(req?.query, req.user?.id);
    ok(res, { message: FOUND, data });
}));

// POST /chats/:id/view
router.post('/:id/view', ValidateNumericParams('id'), AuthenticationMiddleware, AsyncHandler(async (req, res) => {
    await ChatService.seenChatMessage(req.params?.id, req.body, req.user?.id);
    created(res, { message: CREATED });
}));

module.exports = router;
```

**Rules:**
- Every route handler wrapped in `AsyncHandler` — no try/catch in controllers.
- `AuthenticationMiddleware` before every protected route.
- Use numeric param middleware (`ValidateNumericParams`) for all `/:id` routes.
- Respond via `ok()` / `created()` from `ResponseUtils` — never `res.json()` directly.
- Use message constants from `utils/Messages` — no hardcoded strings.

### Service

```js
const logger = require('../config/logger');
const Repository = require('../common/Repository');

class ChatService {
    static async findAllChatsByUserId(query, userId) {
        // business logic here
        logger.info(`Fetching chats for user ${userId}`);
        return Repository.findAll(...);
    }
}

module.exports = ChatService;
```

**Rules:**
- Always `logger.info/warn/error()` — never `console.log`.
- Static methods on a class — no exported plain functions for services.
- Business logic only — no `req`/`res` objects in services.

---

## React / Client

### Data Fetching with React Query

```jsx
// useQuery — read
const { data, isLoading } = useQuery({
    queryKey: ['blogs', 'all', searchTerm, sortBy],
    queryFn: () => getAllBlogs({ search: searchTerm, sort: sortBy }),
});

// useMutation — write
const queryClient = useQueryClient();

const createMutation = useMutation({
    mutationFn: createBlog,
    onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: ['blogs'] });
        toastify(TOAST_TYPE.SUCCESS, 'Blog post created successfully');
        reset();
    },
    onError: (err) => handleErrors(err, setError),
});
```

**Rules:**
- Query keys follow `['entity', 'scope', ...filters]` — always pluralize base key.
- Every mutation has `onSuccess` that calls `invalidateQueries` on the affected key.
- Every mutation has `onError` that calls `handleErrors(err, setError)`.
- Show `<PageLoadingOverlay />` while `isLoading` is true on page-level queries.
- Use `isPending` from the mutation for submit button loading state.

### Form with Validation

```jsx
const schema = z.object({
    title: z.string().min(1, 'Title is required').max(255),
    status: z.enum(['DRAFT', 'PUBLISHED']).default('DRAFT'),
});

const { register, handleSubmit, reset, control, setError, formState: { errors } } = useForm({
    resolver: zodResolver(schema),
    defaultValues: { title: '', status: 'DRAFT' },
});

const onSubmit = (data) => createMutation.mutate(data);
```

**Rules:**
- Always Zod schema + `zodResolver` — no manual validation logic.
- `setError` passed to `handleErrors` for backend validation error mapping.
- `reset()` called in `onSuccess` to clear the form after submission.

### Error Handling & Toasts

```jsx
import { toastify } from '@/common/toastify';
import { TOAST_TYPE } from '@/constants/app.constants';
import { handleErrors } from '@/utils/ErrorUtils';

// Success toast
toastify(TOAST_TYPE.SUCCESS, 'Created successfully');

// Error toast (non-form)
toastify(TOAST_TYPE.ERROR, 'Something went wrong');

// Map backend validation errors to form fields
onError: (err) => handleErrors(err, setError)
```

**Rules:**
- Never use `alert()` or `console.log` in production code.
- Use `toastify` for all user-facing feedback.
- Use `handleErrors` for all mutation error handling where a form's `setError` is available.

### Tailwind Class Merging

```jsx
import { cn } from '@/lib/utils';

// Always use cn() for conditional or merged classes
<button className={cn(
    'px-4 py-2 rounded font-medium',
    isActive && 'bg-blue-600 text-white',
    isDisabled && 'opacity-50 cursor-not-allowed'
)}>
    Submit
</button>
```

**Rule:** Never concatenate Tailwind class strings directly — always `cn(...)`.
