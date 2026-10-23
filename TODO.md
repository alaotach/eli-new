# ELI5 App Improvement Tasks

Based on code review, here are the recommended improvements:

## Priority 1 (High Impact)

### 1. Implement Dependency Injection (Hilt)
- [x] Add Hilt dependencies to build.gradle.kts
- [x] Configure Hilt in Application class
- [ ] Inject ViewModels using @HiltViewModel
- [ ] Inject Repository dependencies
- [ ] Replace manual instantiation with injection

### 2. Add Comprehensive Test Suite
- [ ] Add unit test dependencies (JUnit, Mockito, Coroutines Test)
- [ ] Create unit tests for:
  - ViewModel business logic (OverlayViewModel, MainActivity ViewModel logic)
  - Repository transformations
  - Data model parsing
- [ ] Create instrumented tests for:
  - Critical user flows (onboarding, explanation generation)
  - Navigation flows
- [ ] Add UI tests for Compose components

### 3. Enhance Error Handling & User Experience
- [ ] Create specific error types/messages instead of generic "Error: {message}"
- [ ] Add retry mechanism with exponential backoff for API calls
- [ ] Implement skeleton loaders for better loading states
- [ ] Improve empty/error states throughout the app
- [ ] Add network error handling with user-friendly messages

## Priority 2 (Medium Impact)

### 4. Improve Code Reusability & Maintainability
- [ ] Extract common UI components (buttons, cards, loading indicators) into reusable library
- [ ] Create common extension functions for frequent operations
- [ ] Consolidate spacing, typography, and color constants into centralized files
- [ ] Create base classes for common ViewModel patterns

### 5. Optimize Performance
- [ ] Review database queries for proper indexing
- [ ] Consider implementing response caching for API calls
- [ ] If adding images, implement efficient image loading (Coil/Glide)
- [ ] Optimize recomposition in Compose UI where applicable

## Priority 3 (Lower Impact but Good Practice)

### 6. Move Strings to Resources
- [ ] Extract hardcoded strings to strings.xml for localization support
- [ ] Extract dimension values to dimens.xml
- [ ] Extract color values to colors.xml where appropriate

### 7. Enhance Security Measures
- [ ] Review API key management (ensure not hardcoded in version control)
- [ ] Consider implementing certificate pinning for production
- [ ] Review clipboard access and sensitive text handling

### 8. Add Analytics/Tracking
- [ ] Add basic analytics for user behavior insights
- [ ] Track feature usage and error occurrences

## Completed Tasks
- [x] Initial code review completed