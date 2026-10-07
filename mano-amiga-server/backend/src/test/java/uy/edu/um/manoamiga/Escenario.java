package uy.edu.um.manoamiga;

import uy.edu.um.manoamiga.modelo.Etiqueta;
import uy.edu.um.manoamiga.modelo.Organizacion;

/** 2 organizaciones con su coordinador, 2 voluntarios, 1 judicial, 1 admin y etiquetas (normal y sensible). */
record Escenario(Organizacion orgA, Organizacion orgB, BaseTest.Prueba coordA, BaseTest.Prueba coordB, BaseTest.Prueba vol1,
        BaseTest.Prueba vol2, BaseTest.Prueba judicial, BaseTest.Prueba admin, Etiqueta normal, Etiqueta sensible) {

    static Escenario armar(BaseTest t) {
        Organizacion a = t.crearOrganizacion();
        Organizacion b = t.crearOrganizacion();
        return new Escenario(a, b, t.coordinador(a), t.coordinador(b), t.voluntario(), t.voluntario(), t.judicial(), t.admin(),
                t.crearEtiqueta(false), t.crearEtiqueta(true));
    }
}
