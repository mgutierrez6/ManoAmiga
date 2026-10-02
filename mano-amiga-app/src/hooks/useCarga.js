import { useCallback, useEffect, useRef, useState } from 'react';
import { useFocusEffect } from '@react-navigation/native';
import { mensajeDeError } from '../api/client';

/**
 * Hook genérico para cargar datos: maneja cargando / error / recargar.
 * Se vuelve a cargar cada vez que la pantalla toma foco (por ejemplo, al volver de otra pantalla).
 *   const { datos, cargando, error, recargar } = useCarga(() => listarActividades(), []);
 */
export function useCarga(funcion, deps = []) {
  const [datos, setDatos] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const montado = useRef(true);

  useEffect(() => {
    montado.current = true;
    return () => {
      montado.current = false;
    };
  }, []);

  const recargar = useCallback(async () => {
    setCargando(true);
    setError(null);
    try {
      const r = await funcion();
      if (montado.current) setDatos(r);
    } catch (e) {
      if (montado.current) setError(mensajeDeError(e));
    } finally {
      if (montado.current) setCargando(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useFocusEffect(
    useCallback(() => {
      recargar();
    }, [recargar])
  );

  return { datos, setDatos, cargando, error, recargar };
}
