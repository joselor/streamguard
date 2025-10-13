#!/bin/bash
# StreamGuard Spark ML Pipeline Runner
# Quick start script for running the training data generation pipeline

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=====================================${NC}"
echo -e "${GREEN}StreamGuard Spark ML Pipeline${NC}"
echo -e "${GREEN}=====================================${NC}"

# Check if we're in the right directory
if [ ! -f "src/training_data_generator.py" ]; then
    echo -e "${RED}Error: Must run from spark-ml-pipeline directory${NC}"
    exit 1
fi

# Check if virtual environment exists
if [ ! -d "venv" ]; then
    echo -e "${YELLOW}Creating virtual environment...${NC}"
    python3 -m venv venv
fi

# Activate virtual environment
echo -e "${GREEN}Activating virtual environment...${NC}"
source venv/bin/activate

# Install dependencies if needed
if ! python -c "import pyspark" 2>/dev/null; then
    echo -e "${YELLOW}Installing dependencies...${NC}"
    pip install -r requirements.txt
fi

# Check if Kafka is running
if ! nc -z localhost 9092 2>/dev/null; then
    echo -e "${YELLOW}Warning: Kafka doesn't appear to be running on localhost:9092${NC}"
    echo -e "${YELLOW}Run 'docker-compose up -d kafka' from project root${NC}"
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Parse arguments
CONFIG="config/spark_config.yaml"
MAX_EVENTS=""
OUTPUT=""
START_OFFSET="earliest"

while [[ $# -gt 0 ]]; do
    case $1 in
        --config)
            CONFIG="$2"
            shift 2
            ;;
        --max-events)
            MAX_EVENTS="--max-events $2"
            shift 2
            ;;
        --output)
            OUTPUT="--output $2"
            shift 2
            ;;
        --start-offset)
            START_OFFSET="$2"
            shift 2
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --config PATH         Config file path (default: config/spark_config.yaml)"
            echo "  --max-events N        Maximum events to process"
            echo "  --output PATH         Output directory for training data"
            echo "  --start-offset OFFSET Kafka offset (earliest/latest, default: earliest)"
            echo "  -h, --help            Show this help message"
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# Run pipeline
echo -e "${GREEN}Starting pipeline...${NC}"
echo -e "${YELLOW}Config: $CONFIG${NC}"
echo -e "${YELLOW}Start offset: $START_OFFSET${NC}"

python src/training_data_generator.py \
    --config "$CONFIG" \
    --start-offset "$START_OFFSET" \
    $MAX_EVENTS \
    $OUTPUT

# Check exit code
if [ $? -eq 0 ]; then
    echo -e "${GREEN}=====================================${NC}"
    echo -e "${GREEN}Pipeline completed successfully!${NC}"
    echo -e "${GREEN}=====================================${NC}"

    # Show output location
    if [ -d "output/training_data" ]; then
        echo -e "${GREEN}Output files:${NC}"
        ls -lh output/training_data/
        echo ""
        if [ -f "output/training_data/anomaly_report.json" ]; then
            echo -e "${GREEN}Anomaly report:${NC}"
            cat output/training_data/anomaly_report.json
        fi
    fi
else
    echo -e "${RED}Pipeline failed with error code $?${NC}"
    exit 1
fi