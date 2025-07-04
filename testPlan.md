# MP3Org Application Test Plan - Config Tab StackOverflowError Fix Verification

## 🎯 **Test Objectives**
- [ ] Verify Issue #26 (config tab StackOverflowError) is completely resolved
- [ ] Ensure all profile management functionality works correctly
- [ ] Validate no regressions were introduced by the fix
- [ ] Test edge cases and error conditions

---

## 🏗️ **Test Environment Setup**

### **Prerequisites:**
- [ ] Latest code from main branch (commit 827ed41)
- [ ] Clean build: `./gradlew clean build`
- [ ] Multiple database profiles available for testing
- [ ] Some music files in at least one database for realistic testing

### **Application Startup:**
- [ ] Run: `./gradlew run`
- [ ] **Expected:** Application starts without errors, no StackOverflowError during initialization

---

## 📋 **Core Test Scenarios**

### **Test 1: Basic Config Tab Access** ⭐ **CRITICAL**
**Objective:** Verify the primary issue is fixed

- [ ] Launch application
- [ ] Click on "Config" tab
- [ ] Observe config tab loads properly
- [ ] Switch between other tabs (Duplicate Manager, Metadata Editor, Import & Organize)
- [ ] Return to Config tab multiple times

**Expected Results:**
- [ ] ✅ Config tab opens without StackOverflowError
- [ ] ✅ No infinite loop or application freeze
- [ ] ✅ Tab switching is smooth and responsive
- [ ] ✅ No error messages in console/logs

---

### **Test 2: Profile Switching Functionality** ⭐ **CRITICAL**
**Objective:** Verify profile switching works correctly after the fix

- [ ] Go to Config tab → Database Profiles section
- [ ] Note current active profile in dropdown
- [ ] Select a different profile from dropdown
- [ ] Confirm the switch in the dialog that appears
- [ ] Verify profile information updates (name, path, music file count)
- [ ] Switch back to original profile
- [ ] Test switching between 3+ different profiles rapidly

**Expected Results:**
- [ ] ✅ Profile switching completes successfully
- [ ] ✅ Profile information updates correctly
- [ ] ✅ Music file counts display properly
- [ ] ✅ No recursive calling or infinite loops
- [ ] ✅ Status messages show "Switched to profile: [name]"

---

### **Test 3: Profile Management Operations**
**Objective:** Ensure all profile operations work correctly

#### **3a. Create New Profile**
- [ ] Click "New Profile" button
- [ ] Enter profile name: "Test Profile [timestamp]"
- [ ] Select database directory
- [ ] Verify new profile appears in dropdown
- [ ] Verify new profile becomes active

#### **3b. Duplicate Profile**
- [ ] Select an existing profile
- [ ] Click "Duplicate Profile" button
- [ ] Accept default name or modify
- [ ] Verify duplicated profile appears
- [ ] Switch to duplicated profile

#### **3c. Rename Profile**
- [ ] Select a test profile
- [ ] Click "Rename Profile" button
- [ ] Enter new name: "Renamed Test Profile"
- [ ] Verify name updates in dropdown and info panel

#### **3d. Delete Profile**
- [ ] Ensure you have multiple profiles
- [ ] Select a test profile (not your main data)
- [ ] Click "Delete Profile" button
- [ ] Confirm deletion in dialog
- [ ] Verify profile removed from dropdown
- [ ] Verify application automatically switches to another profile

**Expected Results for All 3a-3d:**
- [ ] ✅ All operations complete without errors
- [ ] ✅ UI updates reflect changes immediately
- [ ] ✅ No StackOverflowError during any operation
- [ ] ✅ Profile dropdown stays consistent

---

### **Test 4: Rapid Tab Switching** ⭐ **STRESS TEST**
**Objective:** Test for any remaining circular dependency issues

- [ ] Rapidly switch between tabs: Config → Duplicate Manager → Config → Metadata Editor → Config
- [ ] Repeat this pattern 10 times quickly
- [ ] While in Config tab, rapidly switch between internal config tabs (if any)
- [ ] Switch profiles while rapidly switching main tabs

**Expected Results:**
- [ ] ✅ No application freezing or slowdown
- [ ] ✅ No StackOverflowError
- [ ] ✅ All tabs load correctly each time
- [ ] ✅ Performance remains responsive

---

### **Test 5: Profile Switching Edge Cases**
**Objective:** Test edge conditions that might trigger the old bug

#### **5a. Same Profile Selection**
- [ ] Note current active profile
- [ ] Select the same profile from dropdown again
- [ ] Verify no unnecessary processing occurs

#### **5b. Profile Switch During Tab Change**
- [ ] Start switching to Config tab
- [ ] Immediately switch profiles before tab fully loads
- [ ] Switch back to another tab while profile switch is in progress

#### **5c. Multiple Profile Switches**
- [ ] Switch to Profile A
- [ ] Immediately switch to Profile B (before first switch completes)
- [ ] Immediately switch to Profile C
- [ ] Verify final state is consistent

**Expected Results for All 5a-5c:**
- [ ] ✅ No errors or exceptions
- [ ] ✅ Application reaches consistent state
- [ ] ✅ Final active profile matches last selection
- [ ] ✅ No recursive calls or infinite loops

---

### **Test 6: Error Condition Testing**
**Objective:** Ensure error handling remains robust

#### **6a. Invalid Profile Operations**
- [ ] Try to delete the last remaining profile
- [ ] Try to create profile with empty name
- [ ] Try to create profile with duplicate name
- [ ] Try to rename profile to existing name

#### **6b. Database Connection Issues**
- [ ] Switch to a profile with corrupted/missing database
- [ ] Try to access Config tab during database errors
- [ ] Verify error messages are appropriate

**Expected Results:**
- [ ] ✅ Appropriate error dialogs shown
- [ ] ✅ Application remains stable
- [ ] ✅ No StackOverflowError even during error conditions
- [ ] ✅ User can recover from error states

---

### **Test 7: Music File Count Display**
**Objective:** Verify the count display that was part of the error chain

- [ ] Switch to a profile with music files
- [ ] Verify music file count displays correctly in profile info
- [ ] Switch to empty profile, verify "0 files" or appropriate message
- [ ] Switch back to profile with files
- [ ] Verify count updates correctly

**Expected Results:**
- [ ] ✅ Music file counts are accurate
- [ ] ✅ Counts update properly when switching profiles
- [ ] ✅ No errors during count retrieval
- [ ] ✅ Formatted display is user-friendly (e.g., "1,234 files")

---

### **Test 8: Application Lifecycle**
**Objective:** Test startup and shutdown behavior

- [ ] Close and restart application multiple times
- [ ] Verify last active profile is remembered
- [ ] Test startup with different profiles as default
- [ ] Ensure clean shutdown with no hanging processes

**Expected Results:**
- [ ] ✅ Consistent startup behavior
- [ ] ✅ Profile state preserved between sessions
- [ ] ✅ No memory leaks or hanging processes
- [ ] ✅ Clean application termination

---

## 🚨 **Red Flag Indicators**

**Immediately stop testing and report if you see:**
- [ ] ❌ StackOverflowError in console
- [ ] ❌ Application freezing or becoming unresponsive
- [ ] ❌ Infinite dialog boxes or repeated error messages
- [ ] ❌ Rapid, repeated log entries indicating loops
- [ ] ❌ Profile information not updating after switches
- [ ] ❌ ComboBox selection jumping or behaving erratically

---

## ✅ **Success Criteria**

**Test passes if:**
- [ ] All 8 test scenarios complete without StackOverflowError
- [ ] Profile switching works smoothly and correctly
- [ ] Config tab is accessible and functional
- [ ] No performance degradation noticed
- [ ] All existing functionality preserved
- [ ] Error handling remains appropriate

---

## 📊 **Test Results Summary**

### **Test Execution Results - [Date/Time]**

#### **Core Tests:**
- [ ] Test 1 - Basic Config Tab Access: ✅ PASS / ❌ FAIL
- [ ] Test 2 - Profile Switching: ✅ PASS / ❌ FAIL  
- [ ] Test 3 - Profile Management: ✅ PASS / ❌ FAIL
- [ ] Test 4 - Rapid Tab Switching: ✅ PASS / ❌ FAIL
- [ ] Test 5 - Edge Cases: ✅ PASS / ❌ FAIL
- [ ] Test 6 - Error Conditions: ✅ PASS / ❌ FAIL
- [ ] Test 7 - Music File Count: ✅ PASS / ❌ FAIL
- [ ] Test 8 - Application Lifecycle: ✅ PASS / ❌ FAIL

#### **Overall Assessment:**
- [ ] **Overall Result:** ✅ PASS / ❌ FAIL
- [ ] **Issues Found:** [List any issues discovered]
- [ ] **Notes:** [Additional observations]
- [ ] **Estimated Testing Time:** 15-20 minutes for complete test suite

---

## 📝 **Test Notes Section**

**Use this space to record observations, issues, or additional notes during testing:**

```
[Your testing notes here]
```

---

**Test Plan Version:** 1.0  
**Created:** 2025-07-04  
**Target Fix:** Issue #26 - Config Tab StackOverflowError  
**Related Commits:** 827ed41, c62a64c, 9862455