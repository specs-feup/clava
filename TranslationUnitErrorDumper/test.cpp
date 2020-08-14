int foo3(X a) {
	//a.bar = "ee"
	fooo();
    return a.bar;
}

int g_build_filename(X a, H b, A c) {
	return 0;
}

int gwy_resource_class_mkdir(GwyResourceClass *klass)
{
    gchar *path;
    gint ok;

    //g_return_val_if_fail(GWY_IS_RESOURCE_CLASS, FALSE);

    path = g_build_filename(gwy_get_user_dir, klass->name, NULL);
    if (g_file_test(path, G_FILE_TEST_IS_DIR)) {
        //g_free(path);
        return TRUE;
    }

    //ok = !g_mkdir(path, 0700);
    //g_free(path);

    return ok;
}
int foo2(X2 a) {
    return a.b;
}



