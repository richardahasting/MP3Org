#!/bin/bash

# Script to find where TMPDIR is being improperly set
# Run as root to check all system and user configuration files

echo "=== TMPDIR Configuration Detective ==="
echo "Searching for TMPDIR settings that might be causing shell issues..."
echo

# Current TMPDIR value
echo "Current TMPDIR value: ${TMPDIR:-'(not set)'}"
echo "Current TEMP value: ${TEMP:-'(not set)'}"
echo "Current TMP value: ${TMP:-'(not set)'}"
echo

# Function to search files and report matches
search_files() {
    local description="$1"
    shift
    local files=("$@")
    
    echo "=== Checking $description ==="
    for file in "${files[@]}"; do
        if [[ -f "$file" ]]; then
            echo "Searching: $file"
            if grep -n -i "tmpdir\|temp\|tmp" "$file" 2>/dev/null; then
                echo "  ^^^ FOUND TMPDIR/TEMP references in $file"
            fi
        else
            echo "Not found: $file"
        fi
    done
    echo
}

# System-wide shell configuration
search_files "System-wide shell configs" \
    "/etc/profile" \
    "/etc/bash.bashrc" \
    "/etc/bashrc" \
    "/etc/zshrc" \
    "/etc/environment" \
    "/etc/login.conf"

# User shell configurations (check for all users)
echo "=== Checking user shell configs ==="
for user_home in /Users/* /home/*; do
    if [[ -d "$user_home" ]]; then
        username=$(basename "$user_home")
        echo "Checking user: $username ($user_home)"
        
        search_files "User configs for $username" \
            "$user_home/.profile" \
            "$user_home/.bashrc" \
            "$user_home/.bash_profile" \
            "$user_home/.zshrc" \
            "$user_home/.zprofile" \
            "$user_home/.zshenv" \
            "$user_home/.config/fish/config.fish"
    fi
done

# macOS specific locations
if [[ "$(uname)" == "Darwin" ]]; then
    echo "=== macOS-specific locations ==="
    search_files "macOS system configs" \
        "/etc/launchd.conf" \
        "/System/Library/LaunchDaemons/com.apple.envvars.plist" \
        "/Library/LaunchDaemons/*.plist" \
        "/Library/LaunchAgents/*.plist"
    
    # Check launchctl environment
    echo "Current launchctl environment:"
    launchctl getenv TMPDIR 2>/dev/null || echo "TMPDIR not set in launchctl"
    launchctl getenv TEMP 2>/dev/null || echo "TEMP not set in launchctl"
    echo
fi

# Application-specific configs that might set TMPDIR
search_files "Application configs" \
    "/usr/local/etc/profile" \
    "/opt/homebrew/etc/profile" \
    "$HOME/.claude/config" \
    "$HOME/.anthropic/config"

# Check for Claude Code specific configurations
echo "=== Claude Code specific checks ==="
if [[ -d "$HOME/.claude" ]]; then
    echo "Found .claude directory, checking contents:"
    find "$HOME/.claude" -type f -name "*.json" -o -name "*.conf" -o -name "settings*" 2>/dev/null | while read -r file; do
        echo "Checking: $file"
        if grep -n -i "tmpdir\|temp\|tmp\|shell" "$file" 2>/dev/null; then
            echo "  ^^^ FOUND relevant references in $file"
        fi
    done
else
    echo ".claude directory not found"
fi
echo

# Check process environment for any Claude-related processes
echo "=== Process environment check ==="
if command -v ps >/dev/null; then
    echo "Looking for Claude-related processes:"
    ps aux | grep -i claude | grep -v grep || echo "No Claude processes found"
    echo
fi

# Check for any scripts or configs that modify shell environment
echo "=== Searching for shell environment modifiers ==="
echo "Looking for files that might export TMPDIR..."

# Search common directories for files containing TMPDIR exports
for dir in /usr/local/bin /usr/local/sbin /opt /usr/share /Library /System/Library; do
    if [[ -d "$dir" ]]; then
        echo "Searching $dir for TMPDIR exports..."
        find "$dir" -type f \( -name "*.sh" -o -name "*.bash" -o -name "*.zsh" -o -name "*.conf" \) -exec grep -l "export.*TMPDIR\|setenv.*TMPDIR" {} \; 2>/dev/null | head -10
    fi
done

echo
echo "=== Summary ==="
echo "Search complete. Look for files that contain:"
echo "1. 'export TMPDIR=...' or 'setenv TMPDIR ...'"
echo "2. References to the problematic path: /var/folders/5c/bkwm1xw50gx921f2lxl3yjn40000gn/T/"
echo "3. Shell initialization scripts that modify TMPDIR"
echo
echo "Common culprits:"
echo "- ~/.zshrc or ~/.bashrc with custom TMPDIR"
echo "- System-wide /etc/profile modifications"
echo "- Application launchers that set environment variables"
echo "- macOS launchctl environment settings"
echo
echo "To fix: Comment out or remove the problematic TMPDIR setting and restart your shell/terminal"