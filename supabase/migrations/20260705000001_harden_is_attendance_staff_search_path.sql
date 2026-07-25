CREATE OR REPLACE FUNCTION public.is_attendance_staff()
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $function$
DECLARE
  v_role text;
BEGIN
  SELECT role INTO v_role
  FROM public.profiles
  WHERE id = auth.uid();

  RETURN v_role IN ('super_admin', 'rois', 'dewan', 'kesantrian');
END;
$function$;

REVOKE ALL ON FUNCTION public.is_attendance_staff() FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.is_attendance_staff() FROM anon;
GRANT EXECUTE ON FUNCTION public.is_attendance_staff() TO authenticated;
GRANT EXECUTE ON FUNCTION public.is_attendance_staff() TO service_role;
