Что происходит внутри класса Index?
- Метод indexAllTxtInPath(String pathToDir), прнимая на вход путь до каталога в файловой системе, запускает алгортм, реализованный в IndexTask, который в свою очередь расчитывает частоту упоминания слов, находящийся в файлах внутри, заданного каталога.
- Метод getInvertedIndex(), после выполнения предыдущего метода возвращает, отсортированную структуру, вида: <"слово", Список:<"количество упоминаний", "путь до файла">>.
- Метод getRelevantDocuments(String term) по переданному слову, возвращает список, в котором указано количество упоминаний в каждом из файлов.
- Метод getMostRelevantDocument(String term) по переданному слову, возвращает количетсво упоминаний, а так же путь до файла, в котором наибольшее число раз упоминается заданное слово.