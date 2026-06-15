const fs = require('fs');
let code = fs.readFileSync('app/src/main/java/com/example/MainActivity.kt', 'utf8');

const oldSearchBar = `                SearchBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onClearSearch = { viewModel.setSearchQuery("") },
                    useRegex = searchUseRegex,
                    onRegexChange = { viewModel.setSearchUseRegex(it) },
                    matchCase = searchMatchCase,
                    onMatchCaseChange = { viewModel.setSearchMatchCase(it) }
                )`;
const newSearchBar = `                SearchBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onClearSearch = { viewModel.setSearchQuery("") }
                )`;
code = code.replace(oldSearchBar, newSearchBar);

const dropdownMenuPart1 = `                                        DropdownMenuItem(
                                            text = { Text("Sắp xếp: Dài → Ngắn") },`;
const newMenuItems = `                                        DropdownMenuItem(
                                            text = { Text("Dùng Regex tìm kiếm") },
                                            leadingIcon = { 
                                                Checkbox(
                                                    checked = searchUseRegex,
                                                    onCheckedChange = null,
                                                ) 
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                viewModel.setSearchUseRegex(!searchUseRegex)
                                            },
                                            modifier = Modifier.testTag("toggle_regex_menu")
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Phân biệt hoa/thường") },
                                            leadingIcon = { 
                                                Checkbox(
                                                    checked = searchMatchCase,
                                                    onCheckedChange = null,
                                                ) 
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                viewModel.setSearchMatchCase(!searchMatchCase)
                                            },
                                            modifier = Modifier.testTag("toggle_match_case_menu")
                                        )
                                        HorizontalDivider()
                                        DropdownMenuItem(
                                            text = { Text("Sắp xếp: Dài → Ngắn") },`;
code = code.replace(dropdownMenuPart1, newMenuItems);

fs.writeFileSync('app/src/main/java/com/example/MainActivity.kt', code);
console.log('Patched MainActivity.kt');
