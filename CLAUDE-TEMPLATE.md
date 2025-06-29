# CLAUDE.md Template - Universal Behaviors

This template contains universal Claude Code behaviors that should be included in every project's CLAUDE.md file.

## Claude Code Automatic Behaviors

**REQUIRED**: Claude must automatically follow these behaviors in every session:

### 1. Developer Log Documentation
- **Record all user prompts** verbatim in developer-log.md (or project-specific log file)
- **Document all work progress** including decisions, implementations, and fixes
- **Update the log throughout each session**, not just at the end
- **Include session statistics** and next steps for continuity

### 2. GitHub Issues Management
- **Create missing labels automatically** when needed for proper categorization
- **Apply comprehensive labeling** to all GitHub issues
- **Create GitHub issues for every fix/improvement** if one doesn't already exist
- **Reference related issues** and dependencies appropriately

### 3. Todo List Management
- **Use TodoWrite/TodoRead** for any multi-step or complex tasks
- **Update task statuses in real-time** as work progresses
- **Mark tasks complete immediately** upon finishing each item

### 4. Issue Tracking for All Changes
- **Check for existing issues** before making any bug fix or improvement
- **Create new GitHub issue** if none exists for the work being done
- **Properly describe the issue** with context and solution approach
- **Reference issues in commits** when implementing fixes
- **Close issues when validated** and working correctly

### 5. Code Quality Standards
- **Follow existing code conventions** and patterns in the codebase
- **Add comprehensive JavaDoc** to new methods and classes
- **Apply consistent formatting** following project standards
- **Run linting and type checking** before completing tasks (when available)

### 6. Testing and Validation
- **Run existing tests** to ensure changes don't break functionality
- **Create tests for new functionality** when appropriate
- **Validate all changes compile** and work as expected

These behaviors ensure complete traceability, proper project management, and comprehensive documentation of all development work.

---

## Usage Instructions

To use this template in a new project:

1. **Copy this section** to your project's CLAUDE.md file
2. **Add project-specific content** above or below this section:
   - Project overview and description
   - Key commands (build, test, run)
   - Architecture and component descriptions
   - Technology stack and dependencies
   - Development notes and conventions

3. **Customize as needed** for project-specific requirements

Example structure:
```markdown
# CLAUDE.md

This file provides guidance to Claude Code when working with [PROJECT NAME].

## Project Overview
[Project-specific description...]

## Key Commands
[Project-specific commands...]

## Architecture
[Project-specific architecture...]

[INSERT CLAUDE CODE AUTOMATIC BEHAVIORS SECTION HERE]
```

This approach maintains consistency across projects while allowing project-specific customization.