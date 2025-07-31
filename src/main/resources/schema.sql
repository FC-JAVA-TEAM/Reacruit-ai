CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS vector_store (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content text,
    metadata json,
    embedding vector(1536)  -- 1536 is the default embedding dimension
);

-- Index creation is now handled by the application code to avoid issues with existing indexes

-- Resume management tables
CREATE TABLE IF NOT EXISTS resumes (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone_number VARCHAR(50) NOT NULL,
    full_text TEXT,
    uploaded_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    file_type VARCHAR(10) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    locked BOOLEAN,
    CONSTRAINT unique_resume UNIQUE (name, email, phone_number)
);

-- Resume vector store table (separate from the main vector_store)
CREATE TABLE IF NOT EXISTS resume_vector_store (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    resume_id uuid NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    content text,
    metadata json,
    embedding vector(1536),
    CONSTRAINT unique_resume_id UNIQUE (resume_id)
);

-- Index creation is now handled by the application code to avoid issues with existing indexes

CREATE TABLE IF NOT EXISTS candidate_evaluations (
    id UUID PRIMARY KEY,
    resume_Id UUID,
    name VARCHAR(255),
    email VARCHAR(255),
    phone_number VARCHAR(50),
    score INTEGER,
    executive_summary TEXT,
    technical_skills INTEGER,
    experience INTEGER,
    education INTEGER,
    soft_skills INTEGER,
    achievements INTEGER,
    recommendation_type VARCHAR(100),
    recommendation_reason TEXT,
    locked BOOLEAN DEFAULT FALSE,
    manager_id VARCHAR(100),
    locked_at TIMESTAMP,
    status VARCHAR(50) DEFAULT 'OPEN',
    custom_status VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS candidate_evaluation_model_key_strengths (
    candidate_evaluation_model_id UUID REFERENCES candidate_evaluations(id),
    key_strengths VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS candidate_evaluation_model_improvement_areas (
    candidate_evaluation_model_id UUID REFERENCES candidate_evaluations(id),
    improvement_areas VARCHAR(255)
);

-- Candidate status history table for audit trail
CREATE TABLE IF NOT EXISTS candidate_status_history (
    id UUID PRIMARY KEY,
    resume_id UUID NOT NULL,
    evaluation_id UUID,
    previous_status VARCHAR(50),
    previous_custom_status VARCHAR(255),
    new_status VARCHAR(50) NOT NULL,
    new_custom_status VARCHAR(255),
    changed_by VARCHAR(255) NOT NULL,
    changed_at TIMESTAMP NOT NULL,
    comments TEXT,
    FOREIGN KEY (resume_id) REFERENCES resumes(id),
    FOREIGN KEY (evaluation_id) REFERENCES candidate_evaluations(id)
);

-- Create indexes for faster queries
CREATE INDEX IF NOT EXISTS idx_candidate_evaluations_resume_id ON candidate_evaluations(resume_id);
CREATE INDEX IF NOT EXISTS idx_candidate_evaluations_locked ON candidate_evaluations(locked);
CREATE INDEX IF NOT EXISTS idx_candidate_evaluations_status ON candidate_evaluations(status);

-- Create indexes for status history
CREATE INDEX IF NOT EXISTS idx_status_history_resume_id ON candidate_status_history(resume_id);
CREATE INDEX IF NOT EXISTS idx_status_history_evaluation_id ON candidate_status_history(evaluation_id);
CREATE INDEX IF NOT EXISTS idx_status_history_changed_by ON candidate_status_history(changed_by);
CREATE INDEX IF NOT EXISTS idx_status_history_changed_at ON candidate_status_history(changed_at);

-- Interviewer management tables
CREATE TABLE IF NOT EXISTS interviewer_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone_number VARCHAR(50),
    experience_years INT NOT NULL,
    interviewer_tier INT NOT NULL,
    max_interviews_per_day INT NOT NULL DEFAULT 3,
    technical_expertise JSONB NOT NULL, -- Store as JSON array
    specializations JSONB NOT NULL,     -- Store as JSON array
    availability JSONB,                 -- Store as JSON object with dates as keys
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Table for interviewer vector store (similar to resume_vector_store)
CREATE TABLE IF NOT EXISTS interviewer_vector_store (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    interviewer_id UUID NOT NULL REFERENCES interviewer_profiles(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    metadata JSONB NOT NULL,
    embedding vector(1536) NOT NULL,
    CONSTRAINT unique_interviewer_id UNIQUE (interviewer_id)
);

-- Create index for vector similarity search
CREATE INDEX IF NOT EXISTS interviewer_vector_idx ON interviewer_vector_store USING ivfflat (embedding vector_cosine_ops);

-- Table for interview assignments
CREATE TABLE IF NOT EXISTS interview_assignments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    interviewer_id UUID NOT NULL REFERENCES interviewer_profiles(id),
    resume_id UUID NOT NULL REFERENCES resumes(id),
    evaluation_id UUID REFERENCES candidate_evaluations(id),
    interview_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED', -- SCHEDULED, COMPLETED, CANCELLED
    feedback TEXT,
    match_score FLOAT,
    match_reasons JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for faster queries
CREATE INDEX IF NOT EXISTS idx_interview_assignments_interviewer ON interview_assignments(interviewer_id);
CREATE INDEX IF NOT EXISTS idx_interview_assignments_resume ON interview_assignments(resume_id);
CREATE INDEX IF NOT EXISTS idx_interview_assignments_date ON interview_assignments(interview_date);
CREATE INDEX IF NOT EXISTS idx_interview_assignments_status ON interview_assignments(status);
