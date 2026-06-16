const fs = require('fs');
let content = fs.readFileSync('app/src/main/java/com/example/MainActivity.kt', 'utf8');
const lines = content.split('\n');
// Truncate file to line 812 (which is index 812, since we keep 0 to 811)
const newContent = lines.slice(0, 812).join('\n');
fs.writeFileSync('app/src/main/java/com/example/MainActivity.kt', newContent);
// Also add imports at the beginning
let modified = fs.readFileSync('app/src/main/java/com/example/MainActivity.kt', 'utf8');
modified = modified.replace('import com.dicteditor.percynguyen92.viewmodel.DictionaryViewModel', 
`import com.dicteditor.percynguyen92.viewmodel.DictionaryViewModel
import com.dicteditor.percynguyen92.ui.components.*
import com.dicteditor.percynguyen92.utils.getFileName`);
fs.writeFileSync('app/src/main/java/com/example/MainActivity.kt', modified);
console.log('Done truncating and adding imports.');
