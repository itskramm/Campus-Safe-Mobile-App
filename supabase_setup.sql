-- Campus Safe Application - Supabase Database Setup
-- Run this in your Supabase SQL Editor: https://app.supabase.com/project/uxafytqyohzjqcbbmyir/sql

-- ============================================
-- 1. CREATE TABLES
-- ============================================

-- Users table (extends Supabase auth.users)
CREATE TABLE IF NOT EXISTS public.users (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  email TEXT UNIQUE NOT NULL,
  full_name TEXT,
  phone_number TEXT,
  biometric_enabled BOOLEAN DEFAULT false,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Hazard Reports table
CREATE TABLE IF NOT EXISTS public.hazard_reports (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
  hazard_type TEXT NOT NULL CHECK (hazard_type IN ('Fire', 'Flood', 'Structural', 'Medical', 'Security', 'Other')),
  description TEXT NOT NULL,
  location TEXT NOT NULL,
  latitude DOUBLE PRECISION NOT NULL,
  longitude DOUBLE PRECISION NOT NULL,
  status TEXT DEFAULT 'Pending' CHECK (status IN ('Pending', 'In Progress', 'Resolved')),
  severity TEXT DEFAULT 'Medium' CHECK (severity IN ('Low', 'Medium', 'High', 'Critical')),
  image_url TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Safety Tips table
CREATE TABLE IF NOT EXISTS public.safety_tips (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title TEXT NOT NULL,
  description TEXT NOT NULL,
  category TEXT NOT NULL CHECK (category IN ('Fire', 'Flood', 'Earthquake', 'Medical', 'Security', 'General')),
  icon_name TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ============================================
-- 2. CREATE INDEXES FOR PERFORMANCE
-- ============================================

CREATE INDEX IF NOT EXISTS idx_hazard_reports_user_id ON public.hazard_reports(user_id);
CREATE INDEX IF NOT EXISTS idx_hazard_reports_status ON public.hazard_reports(status);
CREATE INDEX IF NOT EXISTS idx_hazard_reports_created_at ON public.hazard_reports(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_safety_tips_category ON public.safety_tips(category);

-- ============================================
-- 3. ENABLE ROW LEVEL SECURITY (RLS)
-- ============================================

ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.hazard_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.safety_tips ENABLE ROW LEVEL SECURITY;

-- ============================================
-- 4. CREATE RLS POLICIES
-- ============================================

-- Users Policies
DROP POLICY IF EXISTS "Users can view own profile" ON public.users;
CREATE POLICY "Users can view own profile" ON public.users
  FOR SELECT USING (auth.uid() = id);

DROP POLICY IF EXISTS "Users can update own profile" ON public.users;
CREATE POLICY "Users can update own profile" ON public.users
  FOR UPDATE USING (auth.uid() = id);

DROP POLICY IF EXISTS "Users can insert own profile" ON public.users;
CREATE POLICY "Users can insert own profile" ON public.users
  FOR INSERT WITH CHECK (auth.uid() = id);

-- Hazard Reports Policies
DROP POLICY IF EXISTS "Anyone can view reports" ON public.hazard_reports;
CREATE POLICY "Anyone can view reports" ON public.hazard_reports
  FOR SELECT USING (true);

DROP POLICY IF EXISTS "Users can create reports" ON public.hazard_reports;
CREATE POLICY "Users can create reports" ON public.hazard_reports
  FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update own reports" ON public.hazard_reports;
CREATE POLICY "Users can update own reports" ON public.hazard_reports
  FOR UPDATE USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete own reports" ON public.hazard_reports;
CREATE POLICY "Users can delete own reports" ON public.hazard_reports
  FOR DELETE USING (auth.uid() = user_id);

-- Safety Tips Policies
DROP POLICY IF EXISTS "Anyone can view safety tips" ON public.safety_tips;
CREATE POLICY "Anyone can view safety tips" ON public.safety_tips
  FOR SELECT USING (true);

-- ============================================
-- 5. CREATE FUNCTION TO AUTO-UPDATE updated_at
-- ============================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for hazard_reports
DROP TRIGGER IF EXISTS update_hazard_reports_updated_at ON public.hazard_reports;
CREATE TRIGGER update_hazard_reports_updated_at
    BEFORE UPDATE ON public.hazard_reports
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 6. INSERT SAMPLE SAFETY TIPS
-- ============================================

INSERT INTO public.safety_tips (title, description, category, icon_name) VALUES
  ('Fire Safety', 'Always know the location of fire exits and extinguishers in your building. Never block fire exits or prop open fire doors.', 'Fire', 'ic_fire'),
  ('Emergency Contacts', 'Save campus security number in your phone. In case of emergency, dial 911 or your campus emergency line immediately.', 'General', 'ic_phone'),
  ('Walk in Groups', 'Especially at night, walk with friends or use campus escort services. There is safety in numbers.', 'Security', 'ic_person'),
  ('Report Suspicious Activity', 'If you see something suspicious, report it to campus security immediately. Trust your instincts.', 'Security', 'ic_shield_check'),
  ('Stay Alert', 'Avoid distractions like headphones or phone when walking alone, especially at night. Be aware of your surroundings.', 'Security', 'ic_bell'),
  ('First Aid Basics', 'Know basic first aid and CPR. Attend campus training sessions to be prepared for medical emergencies.', 'Medical', 'ic_medical'),
  ('Weather Alerts', 'Sign up for campus emergency alerts and weather notifications. Stay informed about severe weather conditions.', 'General', 'ic_weather'),
  ('Secure Your Belongings', 'Never leave valuables unattended in public areas. Lock your dorm room and secure your belongings.', 'Security', 'ic_lock'),
  ('Earthquake Preparedness', 'Drop, Cover, and Hold On during an earthquake. Stay away from windows and heavy objects that could fall.', 'Earthquake', 'ic_hazard'),
  ('Flood Safety', 'Never walk or drive through flooded areas. Just 6 inches of moving water can knock you down.', 'Flood', 'ic_water')
ON CONFLICT DO NOTHING;

-- ============================================
-- 7. CREATE FUNCTION TO AUTO-CREATE USER PROFILE
-- ============================================

CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.users (id, email, full_name)
  VALUES (NEW.id, NEW.email, NEW.raw_user_meta_data->>'full_name');
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Create trigger to auto-create user profile on signup
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ============================================
-- SETUP COMPLETE!
-- ============================================

-- Verify tables were created
SELECT 
  table_name,
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_name = t.table_name) as column_count
FROM information_schema.tables t
WHERE table_schema = 'public' 
  AND table_type = 'BASE TABLE'
  AND table_name IN ('users', 'hazard_reports', 'safety_tips');
