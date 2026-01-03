# FIXES APPLIED - Summary

## 1. ✅ Gemini Model Updated
**Changed:** `gemini-2.0-flash-exp` → `gemini-2.5-flash-lite`
**Files:** [app/build.gradle.kts](app/build.gradle.kts#L37)
**Action:** Rebuild app to use the new model in chatbot

## 2. ✅ Image Caching (Device-Side, No Supabase Egress)
**Status:** Already implemented with Glide library
**Files:**
- [TicketAdapter.java](app/src/main/java/com/example/infrastructureproject/TicketAdapter.java#L234) - Ticket list thumbnails
- [TicketDetailActivity.java](app/src/main/java/com/example/infrastructureproject/TicketDetailActivity.java#L209) - Citizen ticket detail
- [CouncilTicketDetailActivity.java](app/src/main/java/com/example/infrastructureproject/CouncilTicketDetailActivity.java#L383) - Council ticket detail

**How it works:**
- Images download from Supabase ONCE only
- Subsequent loads use device disk cache (0 egress!)
- Memory cache for super-fast repeated views
- `DiskCacheStrategy.ALL` caches both original + resized versions

## 3. ✅ Soft Delete RLS Policies (Fix HTTP 403)
**Problem:** RLS policies blocked UPDATE/DELETE operations on tickets
**Solution:** Created safe policies that allow soft-delete flags per role

**SQL File:** [FIX_TICKET_DELETE_RLS_POLICIES.sql](FIX_TICKET_DELETE_RLS_POLICIES.sql)

**Run this in Supabase SQL Editor to fix HTTP 403 delete errors**

### How Soft Delete Works:
- **Citizen deletes:** Sets `deleted_by_citizen = true`
  - Citizen can no longer see the ticket
  - Council and Engineer can still see it (unless they also delete)
  
- **Council deletes:** Sets `deleted_by_council = true`
  - Council can no longer see the ticket
  - Citizen and Engineer can still see it (unless they also delete)
  
- **Engineer deletes:** Sets `deleted_by_engineer = true`
  - Engineer can no longer see the ticket
  - Citizen and Council can still see it (unless they also delete)

- **Database:** Tickets NEVER actually deleted, always remain in database

### Policies Created:
1. **Citizens can view their non-deleted tickets** - SELECT filtered by `deleted_by_citizen = false`
2. **Citizens can insert new tickets** - INSERT with reporter_id check
3. **Citizens can soft-delete their tickets** - UPDATE with reporter_id check
4. **Council can view non-deleted tickets** - SELECT filtered by `deleted_by_council = false` (uses `is_user_in_role` helper)
5. **Council can update and soft-delete tickets** - UPDATE with council role check
6. **Engineers can view their assigned non-deleted tickets** - SELECT filtered by `deleted_by_engineer = false` and `assigned_engineer_id`
7. **Engineers can update their assigned tickets** - UPDATE with engineer assignment check

## 4. ⚠️ Next Steps After Running SQL

After running [FIX_TICKET_DELETE_RLS_POLICIES.sql](FIX_TICKET_DELETE_RLS_POLICIES.sql):

1. **Rebuild the app:**
   ```bash
   .\gradlew.bat clean assembleDebug
   ```

2. **Test deletes:**
   - Citizen: Delete a ticket → should disappear from citizen dashboard but still visible to council/engineer
   - Council: Delete a ticket → should disappear from council view but still visible to others
   - Engineer: Delete a ticket → should disappear from engineer view but still visible to others

3. **Verify images load:**
   - First load: Downloads from Supabase (uses egress once)
   - Refresh/revisit: Loads from device cache (0 egress)
   - Check logcat for "Image loaded from: DISK_CACHE" or "DATA_DISK_CACHE"

4. **Test chatbot:**
   - Open chatbot
   - Send message
   - Should use `gemini-2.5-flash-lite` model

## Verification Commands

### Check policies in Supabase:
```sql
SELECT policyname, cmd, roles, qual, with_check
FROM pg_policies
WHERE tablename = 'tickets'
ORDER BY policyname;
```

### Test soft delete (replace <TICKET_ID> and <USER_ID>):
```sql
-- Soft delete as citizen
UPDATE public.tickets
SET deleted_by_citizen = true
WHERE id = '<TICKET_ID>'::uuid
  AND reporter_id = '<USER_ID>'::uuid;

-- Verify: citizen can't see it anymore
SELECT id, ticket_id, status, deleted_by_citizen
FROM public.tickets
WHERE reporter_id = '<USER_ID>'::uuid;

-- Verify: council can still see it
SELECT id, ticket_id, status, deleted_by_citizen, deleted_by_council
FROM public.tickets
WHERE id = '<TICKET_ID>'::uuid;
```

## Summary of Changes

| Issue | Status | Action Required |
|-------|--------|----------------|
| Gemini model wrong | ✅ Fixed | Rebuild app |
| Images not showing | ✅ Already using Glide cache | None |
| HTTP 403 on delete | ✅ SQL ready | Run SQL in Supabase |
| Soft delete logic | ✅ Policies created | Run SQL + rebuild |

**All fixes are complete. Run the SQL file in Supabase, then rebuild and test the app.**
