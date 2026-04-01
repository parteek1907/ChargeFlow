const fs = require('fs');
const path = require('path');

function stripComments(code) {
    // Regex to match:
    // 1. Strings: " (?:\\.|[^"\\])* "
    // 2. Multi-line comments: \/\* [\s\S]*? \*\/
    // 3. Single-line comments: \/\/ .*
    const regex = /("(?:\\.|[^"\\])*")|(\/\*[\s\S]*?\*\/)|(\/\/.*)/g;
    
    return code.replace(regex, (match, string, multiLine, singleLine) => {
        if (string) return string; // Keep strings as-is
        return ""; // Remove comments
    }).replace(/\n\s*\n/g, '\n\n'); // Clean up excessive blank lines
}

function processDirectory(dir) {
    const files = fs.readdirSync(dir);
    for (const file of files) {
        const fullPath = path.join(dir, file);
        if (fs.statSync(fullPath).isDirectory()) {
            processDirectory(fullPath);
        } else if (file.endsWith('.java')) {
            console.log(`Cleaning ${fullPath}...`);
            const content = fs.readFileSync(fullPath, 'utf8');
            const cleaned = stripComments(content);
            fs.writeFileSync(fullPath, cleaned, 'utf8');
        }
    }
}

const targetDir = process.argv[2] || 'src';
processDirectory(targetDir);
console.log("Cleanup complete.");
