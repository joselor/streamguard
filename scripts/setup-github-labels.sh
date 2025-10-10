#!/bin/bash
# scripts/setup-github-labels.sh

echo "🚀 StreamGuard GitHub Label Setup"
echo "================================="
echo ""

# Create labels
./scripts/create-labels.sh

echo ""
echo "📝 Applying labels to existing issues..."
echo ""

# Label Sprint 1 (completed)
gh issue edit 101 --add-label "sprint-1,infrastructure" 2>/dev/null
gh issue edit 102 --add-label "sprint-1,storage" 2>/dev/null
gh issue edit 103 --add-label "sprint-1,java,kafka" 2>/dev/null
gh issue edit 104 --add-label "sprint-1,c++,kafka" 2>/dev/null
gh issue edit 105 --add-label "sprint-1,c++,storage" 2>/dev/null
gh issue edit 106 --add-label "sprint-1,testing" 2>/dev/null

# Label Sprint 2 (active)
gh issue edit 202 --add-label "sprint-2,demo-nice,c++" 2>/dev/null
gh issue edit 203 --add-label "sprint-2,demo-nice,storage" 2>/dev/null
gh issue edit 206 --add-label "sprint-2,demo-critical,java,query-api,job-requirement" 2>/dev/null
gh issue edit 207 --add-label "sprint-2,demo-critical,query-api" 2>/dev/null
gh issue edit 301 --add-label "sprint-2,demo-critical,monitoring,job-requirement,bonus-points" 2>/dev/null
gh issue edit 302 --add-label "sprint-2,demo-critical,monitoring,job-requirement,bonus-points" 2>/dev/null
gh issue edit 305 --add-label "sprint-2,demo-nice,infrastructure,bonus-points" 2>/dev/null

echo "✅ Labels applied to existing issues"
echo ""

echo "📋 Closing obsolete issues..."
echo ""

# Close obsolete issues
gh issue close 201 --reason "not planned" -c "Obsolete: Not needed for demo" 2>/dev/null
gh issue close 204 --reason "not planned" -c "Obsolete: Replaced by simpler approach" 2>/dev/null
gh issue close 205 --reason "not planned" -c "Obsolete: Replaced by AI detection" 2>/dev/null
gh issue close 209 --reason "not planned" -c "Out of scope for demo" 2>/dev/null
gh issue close 303 --reason "not planned" -c "Obsolete: Load testing not needed" 2>/dev/null
gh issue close 304 --reason "not planned" -c "Obsolete: Basic handling sufficient" 2>/dev/null
gh issue close 310 --reason "not planned" -c "Obsolete: Replaced by US-210" 2>/dev/null
gh issue close 311 --reason "not planned" -c "Obsolete: Too complex for demo" 2>/dev/null

echo "✅ Obsolete issues closed"
echo ""

echo "================================="
echo "✅ GitHub label setup complete!"
echo ""
echo "Next steps:"
echo "1. View labels: gh label list"
echo "2. Create new AI issues (US-210 through US-215)"
echo "3. Start Sprint 2!"